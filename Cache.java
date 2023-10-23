import java.util.*;
import java.io.*;

public class Cache {
    private final Map<String, byte[]> cache;
    // private final Map<String, File> requestedFile = null;
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
        // this.requestedFile.put(key, requestedFile);
    }

    public byte[] get(String key) {
        return cache.get(key);
    }

    // public File getFile(String key){
    //     return requestedFile.get(key);
    // }
}

