import java.util.*;

public class HttpRequest {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers = new HashMap<>();
    private String body;

    public void setMethod(String method) {
        this.method = method;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getMethod() {
        return this.method;
    }

    public String getPath() {
        int i = path.indexOf('?');
        if (i > 0) { // if there is a query string
            return path.substring(0, i);
        }
        return this.path;
    }

    public String getVersion() {
        return this.version;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public String getBody() {
        return this.body;
    }

    public String getQueryString() {
        int i = path.indexOf('?');
        if (i > 0) {
            return path.substring(i + 1);
        }
        return "";
    }

    public boolean isPostRequest() {
        return "POST".equalsIgnoreCase(this.method);
    }
}