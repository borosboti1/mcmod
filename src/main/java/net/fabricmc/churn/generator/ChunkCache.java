package net.fabricmc.churn.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple LRU cache for chunk serialized bytes to limit memory usage.
 */
public class ChunkCache {
    private static final int MAX_CACHED_CHUNKS = 1000;
    private static final Map<String, byte[]> cache = new LinkedHashMap<String, byte[]>(MAX_CACHED_CHUNKS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > MAX_CACHED_CHUNKS;
        }
    };

    public static synchronized byte[] get(String key) {
        return cache.get(key);
    }

    public static synchronized void put(String key, byte[] value) {
        if (key == null || value == null) return;
        cache.put(key, value);
    }

    public static synchronized int size() {
        return cache.size();
    }
}
