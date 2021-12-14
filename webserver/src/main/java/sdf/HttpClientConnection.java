package sdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HttpClientConnection implements Runnable {
    private Socket socket;
    private List<Path> docPaths;
    private BufferedReader reader;
    private HttpWriter writer;
    private final String OK_RESPONSE_HEADER = "HTTP/1.1 200 OK";
    private final String METHOD_NOT_ALLOWED_HEADER = "HTTP/1.1 405 Method Not Allowed";
    private final String RESOURCE_NOT_FOUND_HEADER = "HTTP/1.1 404 Not Found";
    private final String DEFAULT_PAGE_RESOURCE = "/index.html";

    public HttpClientConnection(Socket socket, List<Path> docPaths) {
        this.socket = socket;
        this.docPaths = docPaths;
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new HttpWriter(socket.getOutputStream());

        } catch (IOException e) {
            closeAll(this.socket, this.reader, this.writer, e);
        }
    }

    @Override
    public void run() {
        String clientRequest;

        while (this.socket.isConnected()) {
            try {
                clientRequest = this.reader.readLine();
                String[] parts = clientRequest.split(" ");
                if (parts.length > 0) {
                    String method = parts[0];
                    System.out.printf("Method: %s%n", method);
                    if ("GET".equals(method)) {
                        if (parts.length > 1) {
                            String resource = parts[1];
                            getResource(resource);
                            closeAll(this.socket, this.reader, this.writer, null);
                            break;
                        }
                    } else {
                        // TODO send method not allowed response
                        sendMethodNotAllowedResponse(method);
                    }
                }
                System.out.println(clientRequest);
            } catch (IOException e) {
                closeAll(this.socket, this.reader, this.writer, e);
            }
        }

    }

    private void getResource(String resource) {
        System.out.printf("Client requested: %s%n", resource);
        File resourceFile;
        if ("/".equals(resource)) {
            resource = DEFAULT_PAGE_RESOURCE;
        }
        resourceFile = new File(resource);
        for (Path docPath : docPaths) {
            File directory = docPath.toFile();
            File[] files = directory.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                System.out.println(file.getName().toString());
                if (file.getName().equals(resourceFile.getName())) {
                    try {
                        sendOkResourceResponse(file);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        sendResourceNotFoundResponse(resource);
    }

    private void sendOkResourceResponse(File resourceFile) {
        try {
            this.writer.writeString(OK_RESPONSE_HEADER);
            this.writer.writeString(); // empty line
            byte[] content = Files.readAllBytes(resourceFile.toPath());
            this.writer.writeBytes(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMethodNotAllowedResponse(String method) {
        try {
            this.writer.writeString(METHOD_NOT_ALLOWED_HEADER);
            this.writer.writeString(); // empty line
            this.writer.writeString(String.format("%s not supported", method));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendResourceNotFoundResponse(String resource) {
        try {
            this.writer.writeString(RESOURCE_NOT_FOUND_HEADER);
            this.writer.writeString(); // empty line
            this.writer.writeString(String.format("%s not found", resource));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeAll(Socket socket, BufferedReader reader, HttpWriter writer, Exception exception) {
        if (exception != null) {
            exception.printStackTrace();
        }
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Closed client connection");
    }
}
