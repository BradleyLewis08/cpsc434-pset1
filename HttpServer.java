import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpServer {

    private static final int PORT = 8080;
    private static final boolean debug = true;

    public static void main(String[] args) throws IOException {
        // create new server and client sockets
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server listening at: " + PORT);

        while (true) {
            try {
                // Accepts incoming connections - TODO - multi-threading
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Handle incoming request
                HttpRequestHandler requestHandlerTask = new HttpRequestHandler(clientSocket, "./");

                Thread workerThread = new Thread(requestHandlerTask);
                workerThread.start();

            } catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }
}