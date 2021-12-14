package sdf;

import java.util.List;

public class Main {
    private static final int DEFAULT_PORT = 3000;
    private static final String DEFAULT_DOCROOT = "./static";
    private static final List<String> COMMAND_LINE_FLAGS = List.of("--port", "--docRoot");
    private static final String INVALID_PORT_FLAG_MSG = "Please provide a valid port number, eg --port 8080";
    private static final String INVALID_DOCROOT_FLAG_MSG = "Please provide a valid docRoot directory, eg -docRoot ./target:/opt/tmp/www";
    private int port;
    private String docRoot;

    public Main() {
        this.port = DEFAULT_PORT;
        this.docRoot = DEFAULT_DOCROOT;
    }

    public Main(int port, String docRoot) {
        this.port = port;
        this.docRoot = docRoot;
    }

    public Main(String[] args) {
        this.port = DEFAULT_PORT;
        this.docRoot = DEFAULT_DOCROOT;
        parseCommandLineArguments(args);
    }

    private void parseCommandLineArguments(String[] args) {
        if (args != null && args.length > 0) {
            try {
                for (int i = 0; i < args.length; i += 2) {
                    if ("--port".equals(args[i])) {
                        if (COMMAND_LINE_FLAGS.contains(args[i + 1])) {
                            System.out.println(INVALID_PORT_FLAG_MSG);
                            continue;
                        }
                        try {
                            port = Integer.parseInt(args[i + 1]);
                        } catch (NumberFormatException nfe) {
                            System.out.println(INVALID_PORT_FLAG_MSG);
                        }
                    }
                    if ("--docRoot".equals(args[i])) {
                        if (COMMAND_LINE_FLAGS.contains(args[i + 1])) {
                            System.out.println(INVALID_DOCROOT_FLAG_MSG);
                            continue;
                        }
                        docRoot = args[i + 1];
                    }
                }
            } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
                System.out.println("""
                Please provide valid arguments, eg
                --port 8080 --docRoot ./target:/opt/tmp/www
                """);
            }
        }
    }

    public void launch() {
        System.out.printf("Server running at: port=%d docRoot=%s%n", this.port, this.docRoot);
        HttpServer webServer = new HttpServer(this.port, this.docRoot);
        webServer.start();
    }


    public static void main(String[] args) {
        Main main = new Main(args);

        main.launch();
    }
}
