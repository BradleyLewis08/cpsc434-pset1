import java.io.*;
import java.net.*;
import java.util.*;

public class HttpServer {

    private static boolean debug = true;
    private static int port = 8080;
    private static int cacheSize = 0;

    private static Map<String, String> virtualHostMaps = new HashMap<>(); // Map of serverName to rootDirectory
    static String defaultRootDirectory = null;

    private static void printConfig() {
        System.out.println("port: " + port);
        System.out.println("cacheSize: " + cacheSize);
        System.out.println("defaultRootDirectory: " + defaultRootDirectory);
        System.out.println("virtualHostMaps: " + virtualHostMaps);
    }

    public static void main(String[] args) throws IOException {
        // create new server and client sockets

        if (args.length != 2 || !args[0].equals("-config")) {
            System.out.println("Usage: java HttpServer -config <config file>");
            System.exit(1);
        }

        ConfigParser configParser = new ConfigParser(args[1]);
        ServerConfig serverConfig = null;

        try {
            serverConfig = configParser.parseConfig();
        } catch (InvalidConfigException e) {
            System.out.println("Invalid config: " + e);
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error reading config file: " + e);
            System.exit(1);
        }

        port = serverConfig.getPort();
        virtualHostMaps = serverConfig.getVirtualHosts();
        defaultRootDirectory = serverConfig.getDefaultRootDirectory();
        cacheSize = serverConfig.getCacheSize();

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server listening at: " + port);

        if (debug) {
            printConfig();
        }

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();

                // Handle incoming request
                HttpRequestHandler requestHandlerTask = new HttpRequestHandler(clientSocket, defaultRootDirectory,
                        virtualHostMaps);

                Thread workerThread = new Thread(requestHandlerTask);
                workerThread.start();

            } catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }
}