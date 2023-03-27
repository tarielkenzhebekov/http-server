import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HttpServer implements Runnable {
    private static final String WEB_ROOT = "/home/tariel";
    private static final String DEFAULT_FILE = "index.html";
    private static final int PORT = 8080;
    private Socket socket;

    public HttpServer(Socket socket) {
        this.socket = socket;
    }

    public static void main(String[] args) throws IOException {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port: " + PORT + "...");

            while(true) {
                HttpServer myServer = new HttpServer(serverSocket.accept());
                System.out.println("Connection opened. (" + new Date() + ")");

                Thread thread = new Thread(myServer);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Server Connection error: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while (!(line = br.readLine()).isBlank()) {
                requestBuilder.append(line + "\r\n");
            }

            String request = requestBuilder.toString();
            String[] requestLines = request.split("\r\n");
            String[] requestLine = requestLines[0].split(" ");
            String method = requestLine[0];
            String path = requestLine[1];
            String version = requestLine[2];
            String host = requestLines[1].split(" ")[1];

            List<String> headers = new ArrayList<>();
            for (int h = 2; h < requestLines.length; h++) {
                String header = requestLines[h];
                headers.add(header);
            }

            String accessLog = String.format("Client %s, method %s, path %s, version %s, host %s, headers %s",
                    socket.toString(), method, path, version, host, headers.toString());
            System.out.println(accessLog);

            Path filePath = getFilePath(path);
            if (Files.exists(filePath)) {
                if (Files.isDirectory(filePath)){
                    File directory = new File(filePath.toString());
                    String[] contents = directory.list();

                    String relativePath;
                    if ("/".equals(path)) {
                        relativePath = "";
                    } else {
                        relativePath = path;
                    }

                    StringBuilder directoryList = new StringBuilder();
                    directoryList.append("<a href=\"javascript:history.back()\">..</a><br>");

                    for (String content : contents) {
//                        Exclude hidden files
                        if (!content.startsWith(".")) {
                            directoryList.append("<a href=\"" + relativePath + "/" + content + "\">" + content + "</a><br>");
                        }
                    }
                    sendResponse(socket, "200 OK", "text/html", directoryList.toString().getBytes());
                } else {
                    String contentType = guessContentType(filePath);
                    sendResponse(socket, "200 OK", contentType, Files.readAllBytes(filePath));
                }
            } else {
                byte[] notFoundContent = "<h1>404 Not found</h1>".getBytes();
                sendResponse(socket, "404 Not Found", "text/html", notFoundContent);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e);
        }
    }

    private static void sendResponse(Socket client, String status, String contentType, byte[] content) throws IOException {
        OutputStream clientOutput = client.getOutputStream();
        clientOutput.write(("HTTP/1.1 " + status + "\r\n").getBytes());
        clientOutput.write(("ContentType: " + contentType + "\r\n").getBytes());
        clientOutput.write("\r\n".getBytes());
        clientOutput.write(content);
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();
        client.close();
    }

    private static Path getFilePath(String path) {
//        if ("/".equals(path)) {
//            path = "/index.html";
//        }

        return Paths.get(WEB_ROOT, path);
    }

    private static String guessContentType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }
}
