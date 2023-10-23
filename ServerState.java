import java.util.concurrent.atomic.AtomicInteger;

public class ServerState {
    private volatile boolean acceptingRequests = true;
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    public void setAcceptingRequests(boolean acceptingRequests) {
        this.acceptingRequests = acceptingRequests;
    }

    public boolean isAcceptingRequests() {
        return acceptingRequests;
    }

    public AtomicInteger getActiveTasks() {
        return activeTasks;
    }
}
