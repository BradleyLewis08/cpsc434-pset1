import java.text.SimpleDateFormat;
import java.util.*;

public class HttpResponse {
    private String version = "HTTP/1.1";
    private int statusCode;
    private String statusMessage;
    private Map<String, String> headers = new HashMap<>();
    private byte[] body;

    // toString() method for debugging
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(version + " " + statusCode + " " + statusMessage + "\r\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey() + ": " + entry.getValue() + "\r\n");
        }
        sb.append("\r\n");
        if (body != null) {
            sb.append(new String(body));
        }
        return sb.toString();
    }

    public String getVersion() {
        return this.version;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void setDateHeader() {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = formatter.format(new Date());
        headers.put("Date", date);
    }

    public void setServerHeader(String serverName) {
        headers.put("Server", serverName);
    }

    public void setContentTypeHeader(String contentType) {
        headers.put("Content-Type", contentType);
    }

    public void setContentLength(String contentLength) {
        headers.put("Content-Length", contentLength);
    }

    public void setLastModifiedHeader(long lastModified) {
        Date lastModifiedDate = new Date(lastModified);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        // Format the date
        String formattedDate = dateFormat.format(lastModifiedDate);
        headers.put("Last-Modified", formattedDate);
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void setTransferEncodingHeader(String transferEncoding) {
        headers.put("Transfer-Encoding", transferEncoding);
    }

    public static void setCommonHeaders(HttpResponse response) {
        response.setDateHeader();
        // TODO: Add more
    }

    // ------------------ Response Factories ------------------

    public static HttpResponse heartbeat_ok() {
        HttpResponse response = new HttpResponse();
        setCommonHeaders(response);
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        return response;
    }

    public static HttpResponse ok(byte[] content, long lastModified, String mimeType) {
        HttpResponse response = new HttpResponse();
        setCommonHeaders(response);
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.setBody(content);
        response.setLastModifiedHeader(lastModified);
        response.setContentTypeHeader(mimeType);
        response.setContentLength(String.valueOf(content.length));
        return response;
    }

    public static HttpResponse notModified() {
        HttpResponse response = new HttpResponse();
        response.setStatusCode(304);
        response.setStatusMessage("Not Modified");
        return response;
    }

    public static HttpResponse badRequest() {
        HttpResponse response = new HttpResponse();
        response.setStatusCode(400);
        response.setStatusMessage("Bad Request");
        return response;
    }

    public static HttpResponse unauthorized(boolean credentialsMissing) {
        HttpResponse response = new HttpResponse();
        setCommonHeaders(response);
        response.setStatusCode(401);
        response.setStatusMessage("Unauthorized");
        if (credentialsMissing) {
            response.getHeaders().put("WWW-Authenticate",
                    "Basic realm=\"Restricted Files\", charset=\"UTF-8\"");
        }
        return response;
    }

    public static HttpResponse forbidden() {
        HttpResponse response = new HttpResponse();
        setCommonHeaders(response);
        response.setStatusCode(403);
        response.setStatusMessage("Forbidden");
        return response;
    }

    public static HttpResponse notFound() {
        HttpResponse response = new HttpResponse();
        setCommonHeaders(response);
        response.setStatusCode(404);
        response.setStatusMessage("Not Found");
        return response;
    }

    public static HttpResponse notAcceptable() {
        HttpResponse response = new HttpResponse();
        setCommonHeaders(response);
        response.setStatusCode(406);
        response.setStatusMessage("Not Acceptable");
        return response;
    }

    public static HttpResponse internalServerError() {
        HttpResponse response = new HttpResponse();
        setCommonHeaders(response);
        response.setStatusCode(500);
        response.setStatusMessage("Internal Server Error");
        return response;
    }

    public static HttpResponse notAvailable() {
        HttpResponse response = new HttpResponse();
        setCommonHeaders(response);
        response.setStatusCode(503);
        response.setStatusMessage("Service Unavailable");
        return response;
    }
}
