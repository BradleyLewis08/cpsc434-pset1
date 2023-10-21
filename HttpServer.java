import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpServer {

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

    public void sendResponse(Socket clientSocket, HttpResponse response) throws IOException {
        // response.send(clientSocket.getOutputStream());
        OutputStream out = clientSocket.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

        // Write the status line
        writer.write(response.getVersion() + " " + response.getStatusCode() + " " + response.getStatusMessage());
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
        if(response.getBody() != null){
            writer.write(response.getBody());
            writer.flush();
        }
    }
    
    public HttpResponse generateResponse(HttpRequest request) throws IOException {
        HttpResponse response = new HttpResponse();

        // handle GET request
        if(request.getMethod().equalsIgnoreCase("GET")){
            //temporarily just return Hello World
            response.setStatusCode(200);
            response.setStatusMessage("OK");
            response.setDateHeader();
            response.setServerHeader("myserver");
            response.setContentLength("12");
            response.setContentTypeHeader("text/plain");
            response.setBody("Hello World!");

            // Map<String, String> headers = request.getHeaders();
            // // handle GET request
            // if (request.getPath().equals('/')){
            //     response.setContentLength(headers.get("Content-Length"));
            //     response.setContentTypeHeader(headers.get("Content-Type"));
            //     response.setDateHeader();
            // }

        }
        else {
            //handle other requests
            
        }

        return response;

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
                    System.out.println("Request: " + request.getMethod() + " " + request.getPath() + " " + request.getVersion());
                    for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    if(request.getBody() != null) System.out.println(request.getBody());
                }

                HttpResponse response = server.generateResponse(request);

                server.sendResponse(clientSocket, response);

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