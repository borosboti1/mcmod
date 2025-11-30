package net.fabricmc.churn.generator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Worker runnable that extracts chunk data from world files and enqueues
 * results to the MainThreadApplier.
 */
public class Worker implements Runnable {
    private final ChurnWorkQueue queue;
    private final MainThreadApplier applier;
    private final AtomicLong completedCounter;
    private final GeneratorManager manager;
    private final ChunkExtractor extractor;

    public Worker(ChurnWorkQueue queue, MainThreadApplier applier, AtomicLong completedCounter, 
                  GeneratorManager manager, ChunkExtractor extractor) {
        this.queue = queue;
        this.applier = applier;
        this.completedCounter = completedCounter;
        this.manager = manager;
        this.extractor = extractor;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && !manager.isCancelRequested()) {
                // Respect global paused state (e.g., TPS-based throttling)
                while (manager.isWorkersPaused() && !Thread.currentThread().isInterrupted() && !manager.isCancelRequested()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                ChurnTask task = queue.poll();
                if (task == null) {
                    break;
                }

                // Extract actual chunk data from world files using ChunkExtractor
                try {
                    ChunkData chunkData = extractChunkData(task);
                    byte[] serialized = serializeChunkData(chunkData);
                    ChurnBuildResult result = new ChurnBuildResult(task.chunkX, task.chunkZ, serialized);
                    applier.enqueue(result);
                } catch (Exception e) {
                    System.err.println("[Churn] failed to extract chunk " + task.chunkX + "," + task.chunkZ + ": " + e.getMessage());
                }

                // Update metrics
                completedCounter.incrementAndGet();
            }
        } catch (Exception ex) {
            System.err.println("[Churn] Worker error: " + ex);
        }
    }

    private byte[] simulateGenerate(ChurnTask task) {
        // light-weight placeholder for generated content
        String payload = "chunk:" + task.chunkX + "," + task.chunkZ;
        return payload.getBytes();
    }

    private ChunkData extractChunkData(ChurnTask task) throws Exception {
        // Extract chunk data using extractor's main API
        try {
            // Use extractor to pull chunk data from region files
            java.util.List<ChunkData> chunks = extractor.extractChunksInRadius(task.chunkX, task.chunkZ, 0);
            if (chunks != null && !chunks.isEmpty()) {
                return chunks.get(0);
            }
            // Fallback: return empty ChunkData if extraction fails
            ChunkData cd = new ChunkData();
            cd.chunkX = task.chunkX;
            cd.chunkZ = task.chunkZ;
            cd.minY = -64;
            cd.maxY = 320;
            cd.blockCount = 0;
            cd.timestamp = System.currentTimeMillis();
            cd.metadata.put("status", "extraction_failed");
            return cd;
        } catch (Exception e) {
            System.err.println("[Churn] Warning: ChunkExtractor failed, falling back to stub: " + e.getMessage());
            // Fallback: return a stub chunk if extraction completely fails
            ChunkData cd = new ChunkData();
            cd.chunkX = task.chunkX;
            cd.chunkZ = task.chunkZ;
            cd.minY = -64;
            cd.maxY = 320;
            cd.blockCount = 256 * 256 * 384 / 2; // rough estimate
            cd.timestamp = System.currentTimeMillis();
            cd.metadata.put("extracted", "false");
            cd.metadata.put("error", e.getMessage());
            return cd;
        }
    }

    private byte[] serializeChunkData(ChunkData cd) {
        // Simple serialization to JSON bytes
        String json = "{"
                + "\"x\":" + cd.chunkX + ","
                + "\"z\":" + cd.chunkZ + ","
                + "\"blocks\":" + cd.blockCount
                + "}";
        return json.getBytes();
    }
}