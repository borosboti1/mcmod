package net.fabricmc.churn.generator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Worker runnable that takes chunk tasks, simulates generation, and enqueues
 * results to the MainThreadApplier.
 */
public class Worker implements Runnable {
    private final ChurnWorkQueue queue;
    private final MainThreadApplier applier;
    private final AtomicLong completedCounter;
    private final GeneratorManager manager;

    public Worker(ChurnWorkQueue queue, MainThreadApplier applier, AtomicLong completedCounter, GeneratorManager manager) {
        this.queue = queue;
        this.applier = applier;
        this.completedCounter = completedCounter;
        this.manager = manager;
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
                    // no more tasks
                    break;
                }

                // Simulate generation work. In a real implementation this is where
                // you'd call the ChunkGenerator and build the block sections.
                byte[] fake = simulateGenerate(task);

                ChurnBuildResult result = new ChurnBuildResult(task.chunkX, task.chunkZ, fake);

                // Enqueue result for main-thread application
                applier.enqueue(result);

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
}
