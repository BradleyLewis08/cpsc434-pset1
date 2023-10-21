import java.io.*;
import java.net.*;
import java.util.*;

public class HttpServer {

    public class HttpRequest {
        private String method;
        private String path;
        private String version;
        private Map<String, String> headers = new HashMap<>();
        private String body;

        public void setMethod(String method){
            this.method = method;
        }

        public void setPath(String path){
            this.path = path;
        }

        public void setVersion(String version){
            this.version = version;
        }

        public void setHeaders(Map<String, String> headers){
            this.headers = headers;
        }

        public void setBody(String body){
            this.body = body;
        }
        
    }

    private static final int PORT = 8080;
    private static final boolean debug = true;

    public HttpRequest handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Parse the request line
        String requestLine = in.readLine();
        String[] requestParts = requestLine.split(" ");
        String method = requestParts[0];
        String path = requestParts[1];
        String version = requestParts[2];

        // Parse the headers
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while((headerLine = in.readLine()) != null && !headerLine.isEmpty()){
            String[] headerParts = headerLine.split(": ");
            headers.put(headerParts[0], headerParts[1]);
        }

        // Parse the body if it exists
        String body = null;
        if(headers.containsKey("Content-Length")){
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            char[] bodyChars = new char[contentLength];
            in.read(bodyChars, 0, contentLength);
            body = new String(bodyChars);
        }

        // Map the request to a HttpRequest object
        HttpRequest request = new HttpRequest();
        request.setMethod(method);
        request.setPath(path);
        request.setVersion(version);
        request.setHeaders(headers);
        if (body != null) request.setBody(body);

        return request;
    }
    public static void main(String[] args) throws IOException {
        //create new server and client sockets
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server listening at: " + PORT);
        HttpServer server = new HttpServer();

        while(true){
            try {
                // Accepts incoming connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                
                // Handle incoming request
                HttpRequest request = server.handleRequest(clientSocket);

                // Print request to console
                if(debug){
                    System.out.println("Request: " + request.method + " " + request.path + " " + request.version);
                    for (Map.Entry<String, String> entry : request.headers.entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    if(request.body != null) System.out.println(request.body);
                }

                // TODO - Multi-threading to handle multiple clients
                //create new thread for each client
                // ClientHandler clientHandler = new ClientHandler(clientSocket);
                // clientHandler.start();

                clientSocket.close();
            }
            catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }
}