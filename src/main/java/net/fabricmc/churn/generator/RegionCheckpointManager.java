package net.fabricmc.churn.generator;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class RegionCheckpointManager {
    private static RegionCheckpointManager INSTANCE;
    private final Path dir;
    private final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    private RegionCheckpointManager(Path dir) {
        this.dir = dir;
    }

    public static synchronized void init(Path dir) throws IOException {
        if (INSTANCE == null) {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            INSTANCE = new RegionCheckpointManager(dir);
        }
    }

    public static RegionCheckpointManager getInstance() { return INSTANCE; }

    public boolean isChunkDone(int chunkX, int chunkZ) {
        int rx = chunkX >> 5; int rz = chunkZ >> 5;
        String name = rx + "_" + rz + ".chk";
        try {
            Path p = dir.resolve(name);
            if (!Files.exists(p)) return false;
            byte[] b = Files.readAllBytes(p);
            int lx = chunkX & 31; int lz = chunkZ & 31;
            int bit = lx + lz * 32;
            int idx = bit / 8;
            int off = bit % 8;
            if (idx < 0 || idx >= b.length) return false;
            return ((b[idx] >> off) & 1) != 0;
        } catch (IOException e) {
            return false;
        }
    }

    public void markChunkDone(int chunkX, int chunkZ) throws IOException {
        int rx = chunkX >> 5; int rz = chunkZ >> 5;
        String name = rx + "_" + rz + ".chk";
        Path p = dir.resolve(name);
        byte[] b = new byte[128];
        if (Files.exists(p)) b = Files.readAllBytes(p);
        int lx = chunkX & 31; int lz = chunkZ & 31;
        int bit = lx + lz * 32;
        int idx = bit / 8;
        int off = bit % 8;
        b[idx] = (byte)(b[idx] | (1 << off));
        Files.write(p, b);
    }

    public int validateAgainstRegionFiles(Path regionDir) {
        // Simplified: return 0 for OK, positive for mismatches, -1 for error
        try {
            if (!Files.exists(regionDir)) return 0;
            // Not implemented full validation in skeleton
            return 0;
        } catch (Exception e) {
            return -1;
        }
    }
}
