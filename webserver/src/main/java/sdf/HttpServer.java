package sdf;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private int port;
    private String docRoot;
    private List<Path> docPaths = new ArrayList<Path>();
    ServerSocket serverSocket;
    private final int THREAD_POOL_SIZE = 3;

    public HttpServer(int port, String docRoot) {
        this.port = port;
        this.docRoot = docRoot;
    }

    public boolean checkDocPaths() {
        String[] paths = this.docRoot.split(":");
        boolean allPathsReadable = true;
        for (String path : paths) {
            Path docPath = Paths.get(path);
            File docPathFile = docPath.toFile();
            if (docPathFile.isDirectory() && docPathFile.listFiles() != null) {
                docPaths.add(docPath);
            } else {
                if (!docPathFile.exists()) {
                    System.err.printf("%s does not exist%n", path);
                }
                if (!docPathFile.isDirectory()) {
                    System.err.printf("%s is not a directory%n", path);
                }
                if (docPathFile.listFiles() == null) {
                    System.err.printf("%s is not readable%n", path);
                }
                allPathsReadable = false;
            }
        }
        return allPathsReadable;
    }

    public void start() {
        System.out.println("Starting HttpServer...");
        System.out.printf("Visit http://localhost:%d in your browser%n", this.port);
        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!checkDocPaths()) {
            System.err.println("Server is unable to read all the docRoot paths. Quitting...");
            System.exit(1);
        }
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            while (!this.serverSocket.isClosed()) {
                Socket socket = this.serverSocket.accept();
                System.out.println("Listening on port: " + socket.getLocalPort());
                HttpClientConnection client = new HttpClientConnection(socket, this.docPaths);
                System.out.printf("Client connected from %s:%d %n", socket.getInetAddress(), socket.getPort());

                Thread worker = new Thread(client);
                threadPool.submit(worker);
            }
        } catch (IOException e) {
            closeServerSocket();
        }
    }

    private void closeServerSocket() {
        try {
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
