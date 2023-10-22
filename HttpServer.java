import java.io.*;
import java.net.*;
import java.util.*;

public class HttpServer {

    static class VirtualHost {
        String serverName;
        String rootDirectory;
    }

    private static int PORT = 8080;
    private static final boolean debug = true;

    private static Map<String, String> virtualHostMaps = new HashMap<>(); // Map of serverName to rootDirectory
    static String defaultRootDirectory = null;

    public static void config(String configFilePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configFilePath));
        String line;

        VirtualHost currVirtualHost = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("#") || line.isEmpty()) { // Skip comments
                continue;
            }
            if (line.startsWith("Listen")) {
                PORT = Integer.parseInt(line.split(" ")[1]);
            } else if (line.startsWith("<VirtualHost")) {
                currVirtualHost = new VirtualHost();
            } else if (line.startsWith("DocumentRoot") && currVirtualHost != null) {
                currVirtualHost.rootDirectory = line.split(" ")[1];
            } else if (line.startsWith("ServerName") && currVirtualHost != null) {
                currVirtualHost.serverName = line.split(" ")[1];
            } else if (line.startsWith("</VirtualHost>") && currVirtualHost != null) {
                // Add to map
                virtualHostMaps.put(currVirtualHost.serverName, currVirtualHost.rootDirectory);
                // Set default virtual host
                if (defaultRootDirectory == null) {
                    defaultRootDirectory = currVirtualHost.rootDirectory;
                }
                currVirtualHost = null;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        // create new server and client sockets

        if (args.length != 2 || !args[0].equals("-config")) {
            System.out.println("Usage: java HttpServer -config <config file>");
            System.exit(1);
        }

        config(args[1]);

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server listening at: " + PORT);

        while (true) {
            try {
                // Accepts incoming connections - TODO - multi-threading
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client request: " + clientSocket.getInetAddress().getHostAddress());

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