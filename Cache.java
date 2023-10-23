import java.util.*;

public class Cache {
    private final Map<String, byte[]> cache;
    private final int cacheSize;

    public Cache(int cacheSize) {
        this.cacheSize = cacheSize;
        this.cache = new LinkedHashMap<String, byte[]>(cacheSize + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                return size() > cacheSize;
            }
        };
    }

    public void put(String key, byte[] value) {
        cache.put(key, value);
    }

    public byte[] get(String key) {
        return cache.get(key);
    }
}

