import java.text.SimpleDateFormat;
import java.util.*;

public class HttpResponse {
    private String version = "HTTP/1.1";
    private int statusCode;
    private String statusMessage;
    private Map<String, String> headers = new HashMap<>();
    private String body;
    // private byte[] body;

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

    public String getBody() {
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

    public void setBody(String body) {
        this.body = body;
    }

    // need to implement support for chunked transfer encoding instead of
    // content-length
    // not sure if this is correct implementation
    public void setTransferEncodingHeader(String transferEncoding) {
        headers.put("Transfer-Encoding", transferEncoding);
    }
}
