import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer {
    public static AtomicInteger activeTasks = new AtomicInteger(0);
    public static final int MAX_CONCURRENT_REQUESTS = 1;
    private static final int CLIENT_TIMEOUT = 999999;
    private static final ServerState serverState = new ServerState();
    static String defaultRootDirectory = null;

    public static void main(String[] args) throws IOException {
        // create new server and client sockets
        if (args.length != 2 || !args[0].equals("-config")) {
            System.out.println("Usage: java HttpServer -config <config file>");
            System.exit(1);
        }
        ServerConfig serverConfig = null;
        try {
            serverConfig = ConfigParser.parseConfig(args[1]);
        } catch (InvalidConfigException e) {
            System.out.println("Invalid config: " + e);
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error reading config file: " + e);
            System.exit(1);
        }

        ServerSocket serverSocket = new ServerSocket(serverConfig.getPort());
        serverSocket.setSoTimeout(1000);

        // create cache
        Cache cache = new Cache(serverConfig.getCacheSize());

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
                        serverConfig.getDefaultRootDirectory(),
                        serverConfig.getVirtualHosts(), cache, serverState);
                executorService.execute(requestHandlerTask);
            } catch (Exception e) {
                System.out.println("Error sending response: " + e.getMessage());
            }
        }
        executorService.shutdown();
        System.out.println("Waiting for all requests to finish...");
        while (!executorService.isTerminated()) {
            continue;
        }
        System.out.println("All tasks finished.");
        serverSocket.close();
    }
}