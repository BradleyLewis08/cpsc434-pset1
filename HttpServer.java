import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer {

    public static AtomicInteger activeTasks = new AtomicInteger(0);
    public static final int MAX_CONCURRENT_REQUESTS = 1;

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

        // create cache
        Cache cache = new Cache(cacheSize);

        if (debug) {
            printConfig();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();

                if (activeTasks.incrementAndGet() > MAX_CONCURRENT_REQUESTS) {
                    HttpResponse response = HttpResponse.notAvailable();
                    activeTasks.decrementAndGet();
                    OutputStream out = clientSocket.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

                    // Write the status line
                    writer.write(
                            response.getVersion() + " " + response.getStatusCode() + " " + response.getStatusMessage());
                    writer.newLine();

                    // Write the headers
                    for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
                        writer.write(entry.getKey() + ": " + entry.getValue());
                        writer.newLine();
                    }

                    // Blank line
                    writer.newLine();
                    writer.flush();

                    // Write the body
                    if (response.getBody() != null) {
                        out.write(response.getBody());
                        out.flush();
                    }
                } else {
                    // Handle incoming request
                    HttpRequestHandler requestHandlerTask = new HttpRequestHandler(clientSocket, defaultRootDirectory,
                            virtualHostMaps, cache);

                    executorService.execute(requestHandlerTask);
                }
            } catch (Exception e) {
                System.out.println("Error sending response: " + e.getMessage());
            }
        }
    }
}