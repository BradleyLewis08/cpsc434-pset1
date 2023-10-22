import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class HttpRequestHandler implements Runnable {

	private Socket clientSocket;
	private String contentRoot;
	private boolean closeConnectionAfterResponse = false; // Default to keep-alive Connection
	private Map<String, String> virtualHostMap = new HashMap<>(); // Map of serverName to rootDirectory

	public HttpRequestHandler(Socket clientSocket, String contentRoot, Map<String, String> virtualHostMap) {
		this.clientSocket = clientSocket;
		this.contentRoot = contentRoot;
		this.virtualHostMap = virtualHostMap;
	}

	private String maybeRemoveLeadingSlash(String path) {
		if (path.startsWith("/")) {
			return path.substring(1);
		}
		return path;
	}

	private boolean isFileBeyondRoot(String path) {
		try {
			String docRoot = new File(contentRoot).getCanonicalPath();
			String requestedFile = new File(docRoot, path).getCanonicalPath();
			return !requestedFile.startsWith(docRoot);
		} catch (IOException e) {
			System.out.println("Error checking file accessibility: " + e.getMessage());
			return false; // Default to false if there is an error
		}
	}

	public HttpRequest constructRequest() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String requestLine = in.readLine();
			String[] requestParts = requestLine.split(" ");
			String method = requestParts[0];
			String path = maybeRemoveLeadingSlash(requestParts[1]);
			String version = requestParts[2];

			// Parse the headers
			Map<String, String> headers = new HashMap<>();
			String headerLine;
			while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
				String[] headerParts = headerLine.split(": ");
				headers.put(headerParts[0], headerParts[1]);
			}

			// Parse the body if it exists
			String body = null;
			if (headers.containsKey("Content-Length")) {
				int contentLength = Integer.parseInt(headers.get("Content-Length"));
				char[] bodyChars = new char[contentLength];
				in.read(bodyChars, 0, contentLength);
				body = new String(bodyChars);
			}

			if (headers.containsKey("Connection")) {
				String connectionHeader = headers.get("Connection");
				if (connectionHeader.equals("close")) {
					closeConnectionAfterResponse = true;
				}
			}

			if (headers.containsKey("Host")) {
				String hostHeader = headers.get("Host");
				String[] hostParts = hostHeader.split(":");
				String serverName = hostParts[0];
				if (virtualHostMap.containsKey(serverName)) {
					contentRoot = virtualHostMap.get(serverName);
				}
			}

			// Map the request to a HttpRequest object
			HttpRequest request = new HttpRequest();
			request.setMethod(method);
			request.setPath(path);
			request.setVersion(version);
			request.setHeaders(headers);
			if (body != null)
				request.setBody(body);

			return request;
		} catch (IOException e) {
			System.out.println("Error parsing request: " + e.getMessage());
			return null;
		}
	}

	public HttpResponse constructResponse(HttpRequest request) {
		HttpResponse response = new HttpResponse();

		String fileName = request.getPath();

		if (fileName.equals("")) {
			fileName = "index.html"; // default file name
		}

		String pathName = contentRoot + fileName;

		if (isFileBeyondRoot(pathName)) { // Check if the file is outside of the content root
			response.setStatusCode(403);
			response.setStatusMessage("Forbidden");
			response.setBody("<h1>403 Forbidden</h1>");
			return response;
		}

		File requestedFile = new File(pathName);

		if (!requestedFile.exists() || requestedFile.isDirectory()) {
			response.setStatusCode(404);
			response.setStatusMessage("Not Found");
			response.setBody("<h1>404 Not Found</h1>");
		} else {
			response.setStatusCode(200);
			response.setStatusMessage("OK");

			try {
				// TODO: Handle different MIME types
				String content = new String(Files.readAllBytes(requestedFile.toPath()), StandardCharsets.UTF_8);
				response.setBody(content);
			} catch (IOException e) {
				response.setStatusCode(500);
				response.setStatusMessage("Internal Server Error");
				response.setBody("<h1>500 Internal Server Error</h1>");
			}
		}

		response.setDateHeader();
		return response;
	}

	@Override
	public void run() {
		try {
			HttpRequest request = constructRequest();
			HttpResponse response = constructResponse(request);
			sendResponse(response);
			if (closeConnectionAfterResponse) {
				clientSocket.close();
			}
		} catch (Exception e) {
			System.out.println("Error handling request: " + e.getMessage());
		}
	}

	public void sendResponse(HttpResponse response) {
		try {
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
			if (response.getBody() != null) {
				writer.write(response.getBody());
				writer.flush();
			}
		} catch (Exception e) {
			System.out.println("Error sending response: " + e.getMessage());
		}
	}
}
