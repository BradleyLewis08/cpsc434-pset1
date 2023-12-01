import java.util.concurrent.atomic.AtomicInteger;

public class ServerState {
    private volatile boolean acceptingRequests = true;
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final int MAX_CONCURRENT_REQUESTS = 50;

    public void setAcceptingRequests(boolean acceptingRequests) {
        this.acceptingRequests = acceptingRequests;
    }

    public boolean canAcceptRequests() {
        return activeTasks.get() < MAX_CONCURRENT_REQUESTS;
    }

    public int incrementActiveTasks() {
        return activeTasks.incrementAndGet();
    }

    public int decrementActiveTasks() {
        return activeTasks.decrementAndGet();
    }

    public boolean isAcceptingRequests() {
        return acceptingRequests;
    }

    public AtomicInteger getActiveTasks() {
        return activeTasks;
    }
}
