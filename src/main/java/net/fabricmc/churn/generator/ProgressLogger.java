package net.fabricmc.churn.generator;

import java.util.concurrent.atomic.AtomicLong;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProgressLogger extends Thread {
    private final AtomicLong total;
    private final AtomicLong done;
    private volatile boolean stop = false;
    private final Path logPath;
    private final long maxBytes;
    private final int rotateCount;

    public ProgressLogger(AtomicLong total, AtomicLong done) {
        this(total, done, null, 10_000_000L, 3);
    }

    public ProgressLogger(AtomicLong total, AtomicLong done, String logPath, long maxBytes, int rotateCount) {
        this.total = total;
        this.done = done;
        this.logPath = logPath == null ? null : Path.of(logPath);
        this.maxBytes = maxBytes;
        this.rotateCount = rotateCount;
        setDaemon(true);
    }

    public void requestStop() { stop = true; }

    @Override
    public void run() {
        while (!stop) {
            try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
            long t = total.get();
            long d = done.get();
            double pct = t == 0 ? 0.0 : (d * 100.0 / t);
            String line = String.format("[Churn] progress: %.2f%% (%d/%d)", pct, d, t);
            System.out.println(line);
            if (logPath != null) {
                try {
                    String json = String.format("{\"total\":%d,\"done\":%d,\"percent\":%.2f}\n", t, d, pct);
                    Files.writeString(logPath, json, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    rotateIfNeeded();
                } catch (IOException e) {
                    System.err.println("[Churn] failed to write progress log: " + e);
                }
            }
        }
    }

    private void rotateIfNeeded() {
        try {
            if (Files.exists(logPath) && Files.size(logPath) > maxBytes) {
                for (int i = rotateCount - 1; i >= 0; i--) {
                    Path src = logPath.resolveSibling(logPath.getFileName() + (i == 0 ? "" : ("." + i)));
                    Path dst = logPath.resolveSibling(logPath.getFileName() + "." + (i+1));
                    if (Files.exists(src)) Files.move(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            System.err.println("[Churn] log rotation failed: " + e);
        }
    }
}
