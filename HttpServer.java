import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.channels.*;

public class HttpServer {
    public static AtomicInteger activeTasks = new AtomicInteger(0);
    static String defaultRootDirectory = null;

    public static ServerSocketChannel openServerChannel(int port) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            return serverSocketChannel;
        } catch (IOException e) {
            System.out.println("Error opening server socket channel: " + e.getMessage());
            return null;
        }
    }

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

        Cache cache = new Cache(serverConfig.getCacheSize());
        ServerState serverState = new ServerState();

        ServerSocketChannel serverSocketChannel = openServerChannel(serverConfig.getPort());

        if (serverSocketChannel == null) {
            System.out.println("Error opening server socket channel.");
            System.exit(1);
        }

        // Main thread for accepting connections

        int nSelectLoops = serverConfig.getnSelectLoops();

        List<Dispatcher> dispatchers = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(serverConfig.getnSelectLoops());

        for (int i = 0; i < nSelectLoops; i++) {
            Dispatcher dispatcher = new Dispatcher(serverConfig, cache, serverState);
            executorService.execute(dispatcher);
            dispatchers.add(dispatcher);
        }

        int dispatcherIndex = 0;

        ManagementThread managementThread = new ManagementThread(serverState);
        managementThread.start();

        while (serverState.isAcceptingRequests()) {
            SocketChannel clientChannel = serverSocketChannel.accept(); // Accept new connections

            if (clientChannel != null) {
                clientChannel.configureBlocking(false);
                dispatchers.get(dispatcherIndex % nSelectLoops).registerChannel(clientChannel);
                dispatcherIndex++;
            }
        }
        executorService.shutdown();
        // Create executor service with n threads
    }
}