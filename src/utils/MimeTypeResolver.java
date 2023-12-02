package utils;
import java.util.HashMap;
import java.util.Map;

public class MimeTypeResolver {

	private static final Map<String, String> mimeTypes = new HashMap<>();

	static {
		// Populate the MIME types map.
		mimeTypes.put("html", "text/html");
		mimeTypes.put("htm", "text/html");
		mimeTypes.put("txt", "text/plain");
		mimeTypes.put("css", "text/css");
		mimeTypes.put("js", "application/javascript");
		mimeTypes.put("json", "application/json");
		mimeTypes.put("xml", "application/xml");
		mimeTypes.put("jpg", "image/jpeg");
		mimeTypes.put("jpeg", "image/jpeg");
		mimeTypes.put("png", "image/png");
		mimeTypes.put("gif", "image/gif");
		mimeTypes.put("ico", "image/x-icon");
		mimeTypes.put("pdf", "application/pdf");
		// Add more MIME types based on your requirements.
	}

	public static String getMimeType(String fileName) {
		// Extract file extension
		String extension = "";

		// Find the last dot to get the file's extension.
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
			extension = fileName.substring(i + 1);
		}

		// Return the MIME type from the map, or a default value if not found.
		return mimeTypes.getOrDefault(extension.toLowerCase(), "application/octet-stream");
	}
}
