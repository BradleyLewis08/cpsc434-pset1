import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private final ConcurrentHashMap<String, CacheEntry> cache;
    // private final Map<String, File> requestedFile = null;
    private final int cacheSize;
    private int currentSize;

    public Cache(int cacheSize) {
        this.cacheSize = cacheSize;
        this.cache = new ConcurrentHashMap<String, CacheEntry>();
        this.currentSize = 0;
    }

    public void put(String key, byte[] value) {
        // Check enough space
        if (value.length + currentSize > cacheSize) {
            System.out.println("Not enough space in cache to add " + key + " of size " + value.length + " bytes");
            return;
        }
        cache.put(key, new CacheEntry(value));
        currentSize += value.length;
    }

    public CacheEntry get(String key) {
        return cache.get(key);
    }

    public void removeCacheEntry(String pathName) {
        CacheEntry removed = cache.remove(pathName);
        if (removed != null) {
            currentSize -= removed.getContent().length;
        }
    }
}
