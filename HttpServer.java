import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer {

    public static AtomicInteger activeTasks = new AtomicInteger(0);
    public static final int MAX_CONCURRENT_REQUESTS = 1;
    private static final int CLIENT_TIMEOUT = 3000;
    private static final ServerState serverState = new ServerState();

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
        serverSocket.setSoTimeout(1000);

        // create cache
        Cache cache = new Cache(cacheSize);

        // printConfig();

        // create management thread
        ManagementThread managementThread = new ManagementThread(serverState);
        managementThread.start();

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        while (serverState.isAcceptingRequests()) {
            try {
                Socket clientSocket;
                try {
                    clientSocket = serverSocket.accept();
                } catch (SocketTimeoutException e) {
                    // Periodic timeout to check if server is still accepting requests
                    continue;
                }
                clientSocket.setSoTimeout(CLIENT_TIMEOUT);
                // Handle incoming request
                HttpRequestHandler requestHandlerTask = new HttpRequestHandler(clientSocket,
                        defaultRootDirectory,
                        virtualHostMaps, cache, serverState);
                executorService.execute(requestHandlerTask);
            } catch (Exception e) {
                System.out.println("Error sending response: " + e.getMessage());
            }
        }
        executorService.shutdown();
        System.out.println("Waiting for all tasks to finish...");
        while (!executorService.isTerminated()) {
            continue;
        }
        System.out.println("All tasks finished.");
        serverSocket.close();
    }
}