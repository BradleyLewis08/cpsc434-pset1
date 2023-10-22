import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.*;
import java.text.SimpleDateFormat;

public class HttpRequestHandler implements Runnable {

	private Socket clientSocket;
	private String rootDirectory;
	private boolean keepConnectionOpen = false; // Default to close
	private Map<String, String> virtualHostMap = new HashMap<>(); // Map of serverName to rootDirectory

	// Header Constants

	private static final String HOST_HEADER = "Host";
	private static final String ACCEPT_HEADER = "Accept";
	private static final String USER_AGENT_HEADER = "User-Agent";
	private static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";
	private static final String CONNECTION_HEADER = "Connection";
	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String CONTENT_LENGTH_HEADER = "Content-Length";

	private boolean mobileRequest = false; // Is the request from a mobile device?
	private boolean fetchMobileFallback = false; // If URL is not specified for mobile device, we should fallback to
													// search for index.html before returning 404

	Date ifModifiedSinceDate = null;
	List<String> acceptedMimeTypes = new ArrayList<>();

	public HttpRequestHandler(Socket clientSocket, String rootDirectory, Map<String, String> virtualHostMap) {
		this.clientSocket = clientSocket;
		this.rootDirectory = rootDirectory;
		this.virtualHostMap = virtualHostMap;
	}

	private String maybeRemoveLeadingSlash(String path) {
		if (path.startsWith("/")) {
			return path.substring(1);
		}
		return path;
	}

	private void maybeSetMobileRequest(String userAgent) {
		Pattern MOBILE_PATTERN = Pattern.compile(".*(iPhone|Android|Mobile|webOS).*", Pattern.CASE_INSENSITIVE);
		Matcher matcher = MOBILE_PATTERN.matcher(userAgent);

		if (matcher.matches()) {
			mobileRequest = true;
		}
	}

	private boolean isFileBeyondRoot(String path) {
		try {
			String docRoot = new File(rootDirectory).getCanonicalPath();
			String requestedFile = new File(docRoot, path).getCanonicalPath();
			return !requestedFile.startsWith(docRoot);
		} catch (IOException e) {
			System.out.println("Error checking file accessibility: " + e.getMessage());
			return true; // Default to true if there is an error
		}
	}

	public HttpRequest constructRequest() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String requestLine = in.readLine();
			String[] requestParts = requestLine.split(" ");
			System.out.println("Request parts " + requestParts);
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
			if (headers.containsKey(CONTENT_LENGTH_HEADER)) {
				int contentLength = Integer.parseInt(headers.get(CONTENT_LENGTH_HEADER));
				char[] bodyChars = new char[contentLength];
				in.read(bodyChars, 0, contentLength);
				body = new String(bodyChars);
			}

			if (headers.containsKey(CONNECTION_HEADER)) {
				String connectionHeader = headers.get(CONNECTION_HEADER);
				if (connectionHeader.equals("keep-alive")) {
					keepConnectionOpen = true;
				} else if (connectionHeader.equals("close")) {
					keepConnectionOpen = false;
				}
			}

			if (headers.containsKey(HOST_HEADER)) {
				String hostHeader = headers.get(HOST_HEADER);
				String[] hostParts = hostHeader.split(":");
				String serverName = hostParts[0];
				if (virtualHostMap.containsKey(serverName)) {
					rootDirectory = virtualHostMap.get(serverName);
				}
			}

			if (headers.containsKey(USER_AGENT_HEADER)) {
				String userAgentHeader = headers.get(USER_AGENT_HEADER);
				maybeSetMobileRequest(userAgentHeader);
			}

			if (headers.containsKey(IF_MODIFIED_SINCE_HEADER)) {
				SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
				format.setTimeZone(TimeZone.getTimeZone("GMT"));
				try {
					ifModifiedSinceDate = format.parse(headers.get(IF_MODIFIED_SINCE_HEADER));
				} catch (Exception e) {
					// Ignore the header if it is malformed
					System.out.println("Error parsing date, ignoring If-Modified-Since header " + e.getMessage());
				}
			}

			if (headers.containsKey(ACCEPT_HEADER)) {
				String acceptHeader = headers.get(ACCEPT_HEADER);
				// Create a list of accepted mime types
				String[] acceptParts = acceptHeader.split(",");
				for (String acceptPart : acceptParts) {
					String[] acceptPartParts = acceptPart.split(";");
					acceptedMimeTypes.add(acceptPartParts[0].trim());
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

	private File getFileIfExists(String pathName) {
		File requestedFile = new File(pathName);
		if (!requestedFile.exists()) {
			if (fetchMobileFallback) {
				pathName = pathName.replace("index_m.html", "index.html"); // Try fetching non-mobile version
				requestedFile = new File(pathName);
				if (!requestedFile.exists()) {
					return null;
				}
			} else {
				return null;
			}
		}
		return requestedFile;
	}

	public boolean isMimeTypeAccepted(String mimeType) {
		return acceptedMimeTypes.isEmpty() || acceptedMimeTypes.contains(mimeType);
	}

	// URL is guaranteed to be relative path here
	public HttpResponse setResponseContent(String url, HttpResponse response, HttpRequest request) {
		// Handle some defaults here
		if (url.equals("") || url.endsWith("/")) {
			if (mobileRequest) {
				url = url + "index_m.html";
				fetchMobileFallback = true;
			} else {
				url = url + "index.html";
			}
		}

		String pathName = rootDirectory.endsWith("/") ? rootDirectory + url : rootDirectory + "/" + url;

		if (isFileBeyondRoot(pathName)) {
			return HttpResponse.forbidden();
		}

		File requestedFile = getFileIfExists(pathName);
		if (requestedFile == null) {
			return HttpResponse.notFound();
		} else {
			// We now check if the file is a CGI script
			if (requestedFile.getName().endsWith(".cgi")) {
				// First check if the client can accept text/html
				if (!isMimeTypeAccepted("text/html")) {
					return HttpResponse.notAcceptable();
				}
				return handleCGIRequest(requestedFile, request);
			}
			// Check if the file has been modified since the If-Modified-Since header
			long lastModified = requestedFile.lastModified();
			if (ifModifiedSinceDate != null && lastModified <= ifModifiedSinceDate.getTime()) {
				return HttpResponse.notModified();
			}
			try {
				byte[] data = Files.readAllBytes(requestedFile.toPath());
				String mimeType = MimeTypeResolver.getMimeType(requestedFile.getName());
				// Strict adherence to Accept header
				if (!isMimeTypeAccepted(mimeType)) {
					return HttpResponse.notAcceptable();
				}
				return HttpResponse.ok(data, lastModified, mimeType);
			} catch (IOException e) {
				return HttpResponse.internalServerError();
			}
		}
	}

	public HttpResponse constructResponse(HttpRequest request) {
		HttpResponse response = new HttpResponse();

		String url = request.getPath();
		// Ensure requested path is always relative to content root
		if (url.startsWith("/")) {
			url = url.substring(1);
		}
		return setResponseContent(url, response, request);
	}

	@Override
	public void run() {
		try {
			do {
				HttpRequest request = constructRequest();
				HttpResponse response = constructResponse(request);
				sendResponse(response);
				if (!keepConnectionOpen) {
					clientSocket.close();
				}
			} while (keepConnectionOpen);
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
				out.write(response.getBody());
				out.flush();
			}
		} catch (Exception e) {
			System.out.println("Error sending response: " + e.getMessage());
		}
	}

	public HttpResponse handleCGIRequest(File cgiFile, HttpRequest request) {
		try {
			ProcessBuilder pb = new ProcessBuilder(cgiFile.getAbsolutePath());
			Map<String, String> env = pb.environment();

			// Validate query string

			if (request.getQueryString() == null) {
				return HttpResponse.badRequest();
			}

			env.put("QUERY_STRING", request.getQueryString());
			env.put("REQUEST_METHOD", request.getMethod());

			Process process = pb.start();

			// If the request is POST, write the request body to the CGI script's STDIN
			if ("POST".equalsIgnoreCase(request.getMethod()) && request.getBody() != null) {
				try (OutputStream cgiInput = process.getOutputStream()) {
					cgiInput.write(request.getBody().getBytes(StandardCharsets.UTF_8));
				}
			}

			// Get the output from the CGI script
			byte[] outputBytes;

			try (InputStream cgiOutput = process.getInputStream()) {
				InputStream errorStream = process.getErrorStream();
				if (errorStream.available() > 0) {
					return HttpResponse.internalServerError();
				}
				outputBytes = cgiOutput.readAllBytes();
			}

			int exitCode = process.waitFor();

			// Check if the CGI script returned an error
			if (exitCode != 0) {
				return HttpResponse.internalServerError();
			}

			return HttpResponse.ok(outputBytes, cgiFile.lastModified(), "text/html");
		} catch (Exception e) {
			System.out.println("Error handling CGI request: " + e.getMessage());
			return HttpResponse.internalServerError();
		}
	}
}
