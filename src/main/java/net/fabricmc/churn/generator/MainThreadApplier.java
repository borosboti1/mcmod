package net.fabricmc.churn.generator;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Main-thread applier queue. In a real mod this would be processed on the
 * server main thread (scheduled via tick events) and would apply the generated
 * chunk data into the ServerWorld or write to region files.
 */
public class MainThreadApplier {
    private static final MainThreadApplier INSTANCE = new MainThreadApplier();

    public static MainThreadApplier getInstance() {
        return INSTANCE;
    }

    private final ConcurrentLinkedQueue<ChurnBuildResult> queue = new ConcurrentLinkedQueue<>();

    private MainThreadApplier() {
    }

    public void enqueue(ChurnBuildResult result) {
        queue.add(result);
    }

    /**
     * Apply up to `max` results. This method should be invoked from the server
     * main thread in a tick handler. For the skeleton it simply prints the
     * results to stdout.
     */
    public int applyBatch(int max) {
        int applied = 0;
        while (applied < max) {
            ChurnBuildResult r = queue.poll();
            if (r == null) break;
            // Skip if checkpoint says chunk is already done (avoid duplicate apply)
            try {
                if (RegionCheckpointManager.getInstance() != null && RegionCheckpointManager.getInstance().isChunkDone(r.chunkX, r.chunkZ)) {
                    // already applied previously
                    continue;
                }
            } catch (Exception e) {
                // if checkpoint manager fails, fall through and attempt apply
            }

            // In real code: find or create chunk in ServerWorld and set sections/biomes
            System.out.println("[Churn][Applier] Applying chunk " + r.chunkX + "," + r.chunkZ + " bytes=" + (r.data == null ? 0 : r.data.length));
            try {
                if (RegionCheckpointManager.getInstance() != null) {
                    RegionCheckpointManager.getInstance().markChunkDone(r.chunkX, r.chunkZ);
                }
            } catch (Exception e) {
                System.err.println("[Churn] failed to mark checkpoint for chunk " + r + ": " + e);
            }
            applied++;
        }
        return applied;
    }

    /**
     * Serialize pending applier queue to a simple newline-separated file:
     * chunkX,chunkZ,base64(data)
     */
    public int drainToFile(Path out) throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        ChurnBuildResult r;
        while ((r = queue.poll()) != null) {
            String b64 = r.data == null ? "" : Base64.getEncoder().encodeToString(r.data);
            lines.add(r.chunkX + "," + r.chunkZ + "," + b64);
        }
        if (!lines.isEmpty()) {
            try (BufferedWriter w = Files.newBufferedWriter(out)) {
                for (String l : lines) w.write(l + System.lineSeparator());
            }
        }
        return lines.size();
    }

    /**
     * Load pending applier queue from file created by drainToFile and enqueue into applier.
     */
    public int loadFromFile(Path in) throws IOException {
        int count = 0;
        try (BufferedReader r = Files.newBufferedReader(in)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 3);
                if (parts.length >= 2) {
                    try {
                        int x = Integer.parseInt(parts[0]);
                        int z = Integer.parseInt(parts[1]);
                        // Skip entries already marked done by checkpoints to avoid duplicates
                        if (RegionCheckpointManager.getInstance() != null && RegionCheckpointManager.getInstance().isChunkDone(x, z)) {
                            continue;
                        }
                        byte[] data = parts.length == 3 && !parts[2].isEmpty() ? Base64.getDecoder().decode(parts[2]) : new byte[0];
                        enqueue(new ChurnBuildResult(x, z, data));
                        count++;
                    } catch (NumberFormatException ex) {
                        // skip malformed
                    }
                }
            }
        }
        return count;
    }

    public int pending() { return queue.size(); }
}
