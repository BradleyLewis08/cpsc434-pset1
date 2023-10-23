import java.util.*;

public class Cache {
    private final LinkedHashMap<String, CacheEntry> cache;
    // private final Map<String, File> requestedFile = null;
    private final int cacheSize;

    public Cache(int cacheSize) {
        this.cacheSize = cacheSize;
        this.cache = new LinkedHashMap<String, CacheEntry>(this.cacheSize + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > cacheSize;
            }
        };
    }

    public void put(String key, byte[] value) {
        cache.put(key, new CacheEntry(value));
    }

    public CacheEntry get(String key) {
        return cache.get(key);
    }

    public void removeCacheEntry(String pathName) {
        cache.remove(pathName);
    }
}
