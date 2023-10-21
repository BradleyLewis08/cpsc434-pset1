import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static final int PORT = 8080;
    public static void main(String[] args) throws IOException {
        //create new server and client sockets
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server listening at: " + PORT);

        while(true){
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                
                //Read incoming message and print it
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String line;
                while((line = in.readLine()) != null && !line.isEmpty()){
                    System.out.println(line);
                }

                //create new thread for each client
                // ClientHandler clientHandler = new ClientHandler(clientSocket);
                // clientHandler.start();
            }
            catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }
    // public static void main(String[] args) throws IOException {
    //     ServerSocket ss = new ServerSocket(4999);
    //     Socket s = ss.accept();

    //     System.out.println("Client Connected");

    //     InputStreamReader in = new InputStreamReader(s.getInputStream());
    //     BufferedReader bf = new BufferedReader(in);

    //     String str = bf.readLine();
    //     System.out.println("Client: " + str);

    //     PrintWriter pr = new PrintWriter(s.getOutputStream());
    //     pr.println("Hello Client");
    //     pr.flush();
    // }
}