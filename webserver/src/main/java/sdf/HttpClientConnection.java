package sdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class HttpClientConnection implements Runnable {
    private Socket socket;
    private List<Path> docPaths;
    private BufferedReader reader;
    private HttpWriter writer;
    private final String OK_RESPONSE_HEADER = "HTTP/1.1 200 OK";
    private final String METHOD_NOT_ALLOWED_HEADER = "HTTP/1.1 405 Method Not Allowed";
    private final String RESOURCE_NOT_FOUND_HEADER = "HTTP/1.1 404 Not Found";
    private final String PNG_CONTENT_TYPE_HEADER = "Content-Type: image/png";
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
        if ("/".equals(resource)) {
            resource = DEFAULT_PAGE_RESOURCE;
        }
        for (Path docPath : docPaths) {
            File resourceFile= Paths.get(docPath.toString(), resource).toFile();
            if (resourceFile.isFile()) {
                try {
                    sendOkResourceResponse(resourceFile);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        sendResourceNotFoundResponse(resource);
    }

    private Optional<String> getFileExtension(String filename) {
        return Optional.ofNullable(filename)
            .filter(f -> f.contains("."))
            .map(f -> f.substring(f.indexOf(".") + 1));
    }

    private void sendOkResourceResponse(File resourceFile) {
        Optional<String> opt = getFileExtension(resourceFile.getName().toString());
        boolean isPng = false;
        if (opt.isPresent()) {
            isPng = "png".equalsIgnoreCase(opt.get());
        }
        sendOkResourceResponse(resourceFile, isPng);
    }

    private void sendOkResourceResponse(File resourceFile, boolean isPng) {
        try {
            System.out.printf("Sending %sresource %s to client%n", isPng ? "png image " : "", resourceFile.getAbsolutePath());
            this.writer.writeString(OK_RESPONSE_HEADER);
            this.writer.writeString(); // empty line
            if (isPng) {
                this.writer.writeString(PNG_CONTENT_TYPE_HEADER);
            }
            byte[] content = Files.readAllBytes(resourceFile.toPath());
            this.writer.writeBytes(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMethodNotAllowedResponse(String method) {
        try {
            System.out.printf("Sending %s to client%n", METHOD_NOT_ALLOWED_HEADER);
            this.writer.writeString(METHOD_NOT_ALLOWED_HEADER);
            this.writer.writeString(); // empty line
            this.writer.writeString(String.format("%s not supported", method));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendResourceNotFoundResponse(String resource) {
        try {
            System.out.printf("Sending %s to client%n", RESOURCE_NOT_FOUND_HEADER);
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
