import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.channels.*;

public class HttpServer {
    public static AtomicInteger activeTasks = new AtomicInteger(0);
    public static final int MAX_CONCURRENT_REQUESTS = 1;
    private static final int CLIENT_TIMEOUT = 999999;
    private static final ServerState serverState = new ServerState();
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

        Dispatcher dispatcher = new Dispatcher();
        ServerSocketChannel serverSocketChannel = openServerChannel(serverConfig.getPort());

        if (serverSocketChannel == null) {
            System.out.println("Error opening server socket channel.");
            System.exit(1);
        }

        // // create management thread
        // ManagementThread managementThread = new ManagementThread(serverState);
        // managementThread.start();

        Cache cache = new Cache(serverConfig.getCacheSize());

        serverSocketChannel.register(dispatcher.selector(), SelectionKey.OP_ACCEPT);

        Thread dispatcherThread = new Thread(dispatcher);
        dispatcherThread.start();

        // create cache
        // executorService.shutdown();
        // System.out.println("Waiting for all requests to finish...");
        // while (!executorService.isTerminated()) {
        // continue;
        // }
        // System.out.println("All tasks finished.");
        // serverSocket.close();
    }
}