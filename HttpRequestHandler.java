import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.*;
import java.text.SimpleDateFormat;

public class HttpRequestHandler {

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

	private static int TRANSFER_ENCODING_CHUNK_SIZE = 1024;

	private static String maybeRemoveLeadingSlash(String path) {
		if (path.startsWith("/")) {
			return path.substring(1);
		}
		return path;
	}

	private static boolean maybeMobileRequest(String userAgent) {
		Pattern MOBILE_PATTERN = Pattern.compile(".*(iPhone|Android|Mobile|webOS).*", Pattern.CASE_INSENSITIVE);
		Matcher matcher = MOBILE_PATTERN.matcher(userAgent);
		return matcher.matches();
	}

	private static boolean isFileBeyondRoot(String path, String rootDirectory) {
		try {
			String docRoot = new File(rootDirectory).getCanonicalPath();
			String requestedFile = new File(path).getCanonicalPath();
			return !requestedFile.startsWith(docRoot);
		} catch (IOException e) {
			System.out.println("Error checking file accessibility: " + e.getMessage());
			return true; // Default to true if there is an error
		}
	}

	public static HttpRequest constructRequest(SocketChannel channel) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(4096);
		int bytesRead = channel.read(buffer);
		if (bytesRead == -1) {
			System.out.println("Error reading from channel");
			return null;
		}

		buffer.flip();

		String requestString = StandardCharsets.UTF_8.decode(buffer).toString();

		// Break the request into lines
		String[] requestParts = requestString.split("\r\n");

		// Process the request line
		String requestLine = requestParts[0];

		String[] requestLineParts = requestLine.split(" ");

		if (requestParts.length < 3) {
			System.out.println("Invalid request");
			return null; // Invalid request
		}

		String method = requestLineParts[0];
		String path = maybeRemoveLeadingSlash(requestLineParts[1]);
		String version = requestLineParts[2];

		// Parse the headers
		Map<String, String> headers = new HashMap<>();
		int lineIndex = 1;
		String headerLine;

		while (lineIndex < requestParts.length && !(headerLine = requestParts[lineIndex++]).isEmpty()) {
			String[] headerParts = headerLine.split(": ");
			headers.put(headerParts[0], headerParts[1]);
		}

		// Parse the body if it exists
		String body = null;
		if (headers.containsKey(CONTENT_LENGTH_HEADER)) {
			int contentLength = Integer.parseInt(headers.get(CONTENT_LENGTH_HEADER));

			int bodyStartIndex = requestString.indexOf("\r\n\r\n") + 4; // Skip the blank lines between headers and body

			if (requestString.length() >= bodyStartIndex + contentLength) {
				body = requestString.substring(bodyStartIndex, bodyStartIndex + contentLength);
			} else { // Body is not fully read yet, attempt to read the rest of the body from the
						// channel
				StringBuilder bodyBuilder = new StringBuilder();

				if (bodyStartIndex < requestString.length()) {
					bodyBuilder.append(requestString.substring(bodyStartIndex));
				}

				int remainingBytes = contentLength - bodyBuilder.length();

				ByteBuffer bodyBuffer = ByteBuffer.allocate(remainingBytes);

				while (remainingBytes > 0) {
					bytesRead = channel.read(bodyBuffer);
					if (bytesRead == -1) {
						return null;
					}
					remainingBytes -= bytesRead;
					if (bytesRead > 0) {
						bodyBuffer.flip();
						bodyBuilder.append(StandardCharsets.UTF_8.decode(bodyBuffer).toString());
						bodyBuffer.clear();
					}
				}
				body = bodyBuilder.toString();
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

	private static File getFileIfExists(String pathName, boolean fetchMobileFallback) {
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

	public static boolean isMimeTypeAccepted(String mimeType, List<String> acceptedMimeTypes) {
		return acceptedMimeTypes.isEmpty() || acceptedMimeTypes.contains(mimeType);
	}

	public static String getRequestCredentials(Map<String, String> headers) {
		String credentials = null;
		if (headers.containsKey(AUTHORIZATION_HEADER)) {
			String authHeader = headers.get(AUTHORIZATION_HEADER);
			String[] authParts = authHeader.split(" ");
			if (authParts.length == 2 && authParts[0].equals("Basic")) {
				credentials = authParts[1];
			}
		}
		return credentials;
	}

	public static boolean isAuthorized(String pathName, Map<String, String> headers, String credentials) {
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

	private static String getRootDirectory(Map<String, String> headers, ServerConfig serverConfig) {
		String rootDirectory = serverConfig.getDefaultRootDirectory();
		if (headers.containsKey(HOST_HEADER)) {
			Map<String, String> virtualHostsMap = serverConfig.getVirtualHosts();
			String hostHeader = headers.get(HOST_HEADER);
			String[] hostParts = hostHeader.split(":");
			String serverName = hostParts[0];
			if (virtualHostsMap.containsKey(serverName)) {
				rootDirectory = virtualHostsMap.get(serverName);
			}
		}
		return rootDirectory;
	}

	private static Boolean checkIfNotModified(Map<String, String> headers, long lastModified) {
		Date ifModifiedSinceDate = null;
		if (headers.containsKey(IF_MODIFIED_SINCE_HEADER)) {
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:sszzz", Locale.US);
			format.setTimeZone(TimeZone.getTimeZone("GMT"));
			try {
				ifModifiedSinceDate = format.parse(headers.get(IF_MODIFIED_SINCE_HEADER));
			} catch (Exception e) {
				// Ignore the header if it is malformed
				System.out.println("Error parsing date, ignoring If-Modified-Since header " + e.getMessage());
			}
		}

		if (ifModifiedSinceDate != null && lastModified <= ifModifiedSinceDate.getTime()) {
			if (lastModified <= ifModifiedSinceDate.getTime()) {
				return true;
			}
		}
		return false;
	}

	private static List<String> getAcceptedMimeTypes(Map<String, String> headers) {
		List<String> acceptedMimeTypes = new ArrayList<>();
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
		return acceptedMimeTypes;
	}

	// URL is guaranteed to be relative path here

	private static HttpResponse addConnectionTypeHeader(HttpResponse response, boolean keepConnectionOpen) {
		if (keepConnectionOpen) {
			response.setConnectionTypeHeader("keep-alive");
		}
		return response;
	}

	public static HttpResponse setResponseContent(String url, HttpRequest request, ServerConfig serverConfig,
			Cache cache) {
		boolean mobileRequest = false;
		boolean fetchMobileFallback = false;
		boolean keepConnectionOpen = false;

		Map<String, String> headers = request.getHeaders();

		if (headers.containsKey(USER_AGENT_HEADER)) {
			String userAgentHeader = headers.get(USER_AGENT_HEADER);
			mobileRequest = maybeMobileRequest(userAgentHeader);
		}

		if (headers.containsKey(CONNECTION_HEADER)) {
			String connectionHeader = headers.get(CONNECTION_HEADER);
			if (connectionHeader.equals("keep-alive")) {
				keepConnectionOpen = true;
			}
		}

		if (url.equals("") || url.endsWith("/")) {
			if (mobileRequest) {
				url = url + "index_m.html";
				fetchMobileFallback = true;
			} else {
				url = url + "index.html";
			}
		}

		String rootDirectory = getRootDirectory(headers, serverConfig);

		String pathName = rootDirectory.endsWith("/") ? rootDirectory + url : rootDirectory + "/" + url;

		if (isFileBeyondRoot(pathName, rootDirectory)) {
			return addConnectionTypeHeader(HttpResponse.forbidden(), keepConnectionOpen);
		}

		File requestedFile = getFileIfExists(pathName, fetchMobileFallback);

		if (requestedFile == null) {
			return addConnectionTypeHeader(HttpResponse.notFound(), keepConnectionOpen);
		} else {
			pathName = requestedFile.getPath();
			String credentials = getRequestCredentials(headers);
			if (!isAuthorized(pathName, headers, credentials)) {
				return addConnectionTypeHeader(HttpResponse.unauthorized(credentials == null), keepConnectionOpen);
			}

			if (requestedFile.getName().endsWith(".cgi")) {
				return handleCGIRequest(requestedFile, request, keepConnectionOpen);
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

			boolean notModified = checkIfNotModified(headers, lastModified);

			if (notModified) {
				return addConnectionTypeHeader(HttpResponse.notModified(), keepConnectionOpen);
			}

			try {
				byte[] data = null;
				data = Files.readAllBytes(requestedFile.toPath());
				if (maybeEntry != null) {
					// if file is in cache, there is no need to read from disk
					data = maybeEntry.getContent();
				} else {
					data = Files.readAllBytes(requestedFile.toPath());
					cache.put(pathName, data); // add to cache
				}
				String mimeType = MimeTypeResolver.getMimeType(requestedFile.getName());
				// Strict adherence to Accept header
				List<String> acceptedMimeTypes = getAcceptedMimeTypes(headers);

				if (!isMimeTypeAccepted(mimeType, acceptedMimeTypes)) {
					return addConnectionTypeHeader(HttpResponse.notAcceptable(), keepConnectionOpen);
				}
				return addConnectionTypeHeader(HttpResponse.ok(data, lastModified, mimeType), keepConnectionOpen);
			} catch (IOException e) {
				return addConnectionTypeHeader(HttpResponse.internalServerError(), keepConnectionOpen);
			}
		}
	}

	public static HttpResponse constructResponse(HttpRequest request, ServerConfig serverConfig, Cache cache,
			ServerState serverState) {
		String url = request.getPath();
		// Ensure requested path is always relative to content root
		if (url.startsWith("/")) {
			url = url.substring(1);
		}
		if ("load".equals(url)) {
			boolean keepConnectionOpen = false;
			if (request.getHeaders().get("Connection") != null
					&& request.getHeaders().get("Connection").equals("keep-alive")) {
				keepConnectionOpen = true;
			}

			if (!serverState.canAcceptRequests()) {
				return addConnectionTypeHeader(HttpResponse.notAvailable(), keepConnectionOpen);
			} else {
				return addConnectionTypeHeader(HttpResponse.heartbeat_ok(), keepConnectionOpen);
			}
		}
		return setResponseContent(url, request, serverConfig, cache);
	}

	private static ByteBuffer prepareChunkedHeaders(Map<String, String> headers) {
		StringBuilder headerBuilder = new StringBuilder();
		// Append status line and headers
		headerBuilder.append("HTTP/1.1 200 OK\r\n");
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			headerBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
		}
		headerBuilder.append("\r\n");
		return ByteBuffer.wrap(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));
	}

	public static void sendChunkedResponse(SocketChannel clientChannel, HttpResponse response) {
		try {
			ByteBuffer headerBuffer = prepareChunkedHeaders(response.getHeaders());
			clientChannel.write(headerBuffer); // Write headers

			byte[] body = response.getBody();
			ByteBuffer chunkBuffer;

			int offset = 0;
			while (offset < body.length) {
				int chunkSize = Math.min(TRANSFER_ENCODING_CHUNK_SIZE, body.length - offset);
				String chunkHeader = Integer.toHexString(chunkSize) + "\r\n";
				chunkBuffer = ByteBuffer.wrap(chunkHeader.getBytes(StandardCharsets.UTF_8));
				clientChannel.write(chunkBuffer); // Write chunk size

				chunkBuffer = ByteBuffer.wrap(body, offset, chunkSize);
				clientChannel.write(chunkBuffer); // Write chunk data

				chunkBuffer = ByteBuffer.wrap("\r\n".getBytes(StandardCharsets.UTF_8));
				clientChannel.write(chunkBuffer); // Write end of chunk

				offset += chunkSize;
			}

			// Write final chunk
			chunkBuffer = ByteBuffer.wrap("0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
			clientChannel.write(chunkBuffer);
		} catch (Exception e) {
			System.out.println("Error sending response: " + e.getMessage());
			// Handle exception
		}
	}

	public static void sendResponse(SocketChannel clientChannel, HttpResponse response)
			throws SocketException, IOException {
		// Convert the HttpResponse to a byte array
		// Assuming you have a method in HttpResponse to get the byte representation
		if (response.getHeaders().get("Transfer-Encoding") == "chunked") {
			sendChunkedResponse(clientChannel, response);
			return;
		}

		byte[] responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);

		// Wrap the byte array in a ByteBuffer
		ByteBuffer buffer = ByteBuffer.wrap(responseBytes);

		// Write the buffer to the channel
		while (buffer.hasRemaining()) {
			int bytesWritten = clientChannel.write(buffer);
			if (bytesWritten <= 0) {
				// Handle case where you can't write anymore
				break;
			}
		}
	}

	public static HttpResponse handleCGIRequest(File cgiFile, HttpRequest request, boolean keepConnectionOpen) {
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
				return addConnectionTypeHeader(HttpResponse.internalServerError(), keepConnectionOpen);
			}

			HttpResponse response = HttpResponse.ok(outputBytes, cgiFile.lastModified(), "text/html");
			response.setTransferEncodingHeader("chunked");
			return addConnectionTypeHeader(response, keepConnectionOpen);
		} catch (Exception e) {
			System.out.println("Error handling CGI request: " + e.getMessage());
			return addConnectionTypeHeader(HttpResponse.internalServerError(), keepConnectionOpen);
		}
	}
}
