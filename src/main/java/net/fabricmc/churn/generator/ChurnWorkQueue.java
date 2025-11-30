package net.fabricmc.churn.generator;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChurnWorkQueue {
    private final ConcurrentLinkedQueue<ChurnTask> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(ChurnTask task) {
        queue.add(task);
    }

    public ChurnTask poll() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    /**
     * Drain current queue to a newline-separated file with format `chunkX,chunkZ`.
     * Returns the number of entries written.
     */
    public int drainToFile(Path out) throws IOException {
        List<String> lines = new ArrayList<>();
        ChurnTask t;
        while ((t = queue.poll()) != null) {
            lines.add(t.chunkX + "," + t.chunkZ);
        }
        if (!lines.isEmpty()) {
            try (BufferedWriter w = Files.newBufferedWriter(out)) {
                for (String l : lines) w.write(l + System.lineSeparator());
            }
        }
        return lines.size();
    }

    /**
     * Load a queue from a file created by `drainToFile`.
     */
    public static ChurnWorkQueue loadFromFile(Path in) throws IOException {
        ChurnWorkQueue q = new ChurnWorkQueue();
        try (BufferedReader r = Files.newBufferedReader(in)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    try {
                        int x = Integer.parseInt(parts[0]);
                        int z = Integer.parseInt(parts[1]);
                        q.enqueue(new ChurnTask(x, z));
                    } catch (NumberFormatException ex) {
                        // skip malformed
                    }
                }
            }
        }
        return q;
    }
}
