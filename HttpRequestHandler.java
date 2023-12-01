import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.*;
import java.text.SimpleDateFormat;

public class HttpRequestHandler implements Runnable {

	private Socket clientSocket;
	private String rootDirectory;
	private boolean keepConnectionOpen = false; // Default to close
	private Map<String, String> virtualHostMap = new HashMap<>(); // Map of serverName to rootDirectory
	private Cache cache;
	private String credentials = null;

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

	private static int TRANSFER_ENCODING_CHUNK_SIZE = 1024;

	Date ifModifiedSinceDate = null;
	List<String> acceptedMimeTypes = new ArrayList<>();

	public HttpRequestHandler(Socket clientSocket, String rootDirectory, Map<String, String> virtualHostMap,
			Cache cache, ServerState serverState) {
		this.clientSocket = clientSocket;
		this.rootDirectory = rootDirectory;
		this.virtualHostMap = virtualHostMap;
		this.cache = cache;
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
			String requestedFile = new File(path).getCanonicalPath();
			return !requestedFile.startsWith(docRoot);
		} catch (IOException e) {
			System.out.println("Error checking file accessibility: " + e.getMessage());
			return true; // Default to true if there is an error
		}
	}

	public HttpRequest constructRequest() throws IOException {
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

		if (headers.containsKey(AUTHORIZATION_HEADER)) {
			String authHeader = headers.get(AUTHORIZATION_HEADER);
			String[] authParts = authHeader.split(" ");
			if (authParts.length == 2 && authParts[0].equals("Basic")) {
				credentials = authParts[1];
			}
		}

		if (headers.containsKey(ACCEPT_HEADER)) {
			String acceptHeader = headers.get(ACCEPT_HEADER);
			// Create a list of accepted mime types
			String[] acceptParts = acceptHeader.split(",");
			for (String acceptPart : acceptParts) {
				String[] acceptPartParts = acceptPart.split(";");
				if (acceptPartParts[0].trim().equals("*/*")) {
					acceptedMimeTypes.clear();
					break;
				} else {
					acceptedMimeTypes.add(acceptPartParts[0].trim());
				}
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

	public boolean isAuthorized(String pathName) {
		try {
			// Get the directory of the file
			Path filePath = Paths.get(pathName).toAbsolutePath();
			Path directory = filePath.getParent();

			// Check for .htaccess file in the directory
			Path htaccessPath = directory.resolve(".htaccess");
			File htaccessFile = htaccessPath.toFile();

			if (htaccessFile.exists() && !htaccessFile.isDirectory()) {
				HTAccessParser htaccessParser = new HTAccessParser(htaccessFile);
				if (htaccessParser.isAuthenticationRequired()) {
					if (credentials == null) {
						return false;
					}
					return htaccessParser.authenticate(credentials);
				}
				return true;
			}
			return true;
		} catch (Exception e) {
			// Handle exceptions, possibly by logging
			e.printStackTrace();
		}
		// If we reach here, no auth required
		return true;
	}

	// URL is guaranteed to be relative path here
	public HttpResponse setResponseContent(String url, HttpResponse response, HttpRequest request) {
		if ("load".equals(url)) {
			// Check active tasks
			int activeTasks = HttpServer.activeTasks.get();
			int maxTasks = HttpServer.MAX_CONCURRENT_REQUESTS;

			if (activeTasks > maxTasks) {
				return HttpResponse.notAvailable();
			} else {
				return HttpResponse.heartbeat_ok();
			}
		}
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
			pathName = requestedFile.getPath();
			if (!isAuthorized(pathName)) {
				return HttpResponse.unauthorized(credentials == null);
			}
			// We now check if the file is a CGI script
			if (requestedFile.getName().endsWith(".cgi")) {
				// First check if the client can accept text/html in the first place
				return handleCGIRequest(requestedFile, request);
			}
			// Check if the file has been modified since the If-Modified-Since header
			CacheEntry maybeEntry = cache.get(pathName);
			long lastModified = requestedFile.lastModified();

			// If cache entry was added before the file was modified, remove it
			if (maybeEntry != null) {
				if (maybeEntry.getTimeAdded() < lastModified) {
					cache.removeCacheEntry(pathName);
					maybeEntry = null;
				} else {
					lastModified = maybeEntry.getTimeAdded();
				}
			}

			if (ifModifiedSinceDate != null && lastModified <= ifModifiedSinceDate.getTime()) {
				if (lastModified <= ifModifiedSinceDate.getTime()) {
					return HttpResponse.notModified();
				}
			}

			try {
				byte[] data = null;
				if (maybeEntry != null) {
					// if file is in cache, there is no need to read from disk
					data = maybeEntry.getContent();
				} else {
					data = Files.readAllBytes(requestedFile.toPath());
					cache.put(pathName, data); // add to cache
				}
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
				if (request == null) {
					System.out.println("Null request");
					continue;
				}
				// check if server is at capacity
				if (HttpServer.activeTasks.getAndIncrement() >= HttpServer.MAX_CONCURRENT_REQUESTS) {
					HttpServer.activeTasks.decrementAndGet();
					HttpResponseSender.sendResponse(HttpResponse.notAvailable(), clientSocket.getOutputStream());
					clientSocket.close();
					return;
				}
				HttpResponse response = constructResponse(request);
				try {
					HttpResponseSender.sendResponse(response, clientSocket.getOutputStream());
				} catch (IOException e) {
					keepConnectionOpen = false;
				}
				if (!keepConnectionOpen) {
					clientSocket.close();
				}
			} while (keepConnectionOpen && !clientSocket.isClosed());
		} catch (SocketTimeoutException e) {
			try {
				clientSocket.close();
			} catch (Exception ex) {
				System.out.println("Error closing socket: " + ex.getMessage());
			}
		} catch (Exception e) {
			System.out.println("Error handling request: " + e.getMessage());
			keepConnectionOpen = false;
		} finally {
			HttpServer.activeTasks.decrementAndGet();
		}
	}

	public void sendResponse(HttpResponse response) throws SocketException, IOException {
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
	}

	public HttpResponse handleCGIRequest(File cgiFile, HttpRequest request) {
		try {
			ProcessBuilder pb = new ProcessBuilder(cgiFile.getAbsolutePath());
			Map<String, String> env = pb.environment();

			env.put("QUERY_STRING", request.getQueryString());
			env.put("REQUEST_METHOD", request.getMethod());

			if (request.isPostRequest()) {
				String contentLength = request.getHeaders().get(CONTENT_LENGTH_HEADER);
				if (contentLength != null) {
					env.put("CONTENT_LENGTH", contentLength);
				}
				String contentType = request.getHeaders().get("Content-Type");
				if (contentType != null) {
					env.put("CONTENT_TYPE", contentType);
				}
			}

			Process process = pb.start();

			if (request.isPostRequest() && request.getBody() != null) {
				try (OutputStream cgiInput = process.getOutputStream()) {
					cgiInput.write(request.getBody().getBytes(StandardCharsets.UTF_8));
					cgiInput.flush();
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

			try {
				OutputStream out = clientSocket.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

				// Write the status line and headers
				writer.write("HTTP/1.1 200 OK\r\n");
				writer.write("Date: " + new Date() + "\r\n");
				writer.write("Server: MyHTTPServer\r\n");
				writer.write("Content-Type: text/html\r\n");
				writer.write("Transfer-Encoding: chunked\r\n");
				writer.write("\r\n"); // Write blank line between headers and body
				writer.flush();

				// Write the body
				int offset = 0;

				while (offset < outputBytes.length) {
					int chunkSize = Math.min(TRANSFER_ENCODING_CHUNK_SIZE, outputBytes.length - offset);
					out.write(Integer.toHexString(chunkSize).getBytes(StandardCharsets.UTF_8)); // Send chunk size in
																								// hex
					out.write("\r\n".getBytes(StandardCharsets.UTF_8));
					out.write(outputBytes, offset, chunkSize);
					out.write("\r\n".getBytes(StandardCharsets.UTF_8));
					offset += chunkSize;
				}
				// Final chunk
				out.write("0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
				out.flush();
			} catch (Exception e) {
				System.out.println("Error sending response: " + e.getMessage());
				return HttpResponse.internalServerError();
			}
			// Implement chunked transfer encoding
			return HttpResponse.ok(outputBytes, cgiFile.lastModified(), "text/html");
		} catch (Exception e) {
			System.out.println("Error handling CGI request: " + e.getMessage());
			return HttpResponse.internalServerError();
		}
	}
}
