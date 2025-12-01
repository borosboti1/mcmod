package net.fabricmc.churn.generator;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.churn.ui.ProgressDisplayManager;
import net.fabricmc.churn.ui.ConsoleLogger;
import net.minecraft.server.network.ServerPlayerEntity;

public class GeneratorManager {
    private static final GeneratorManager INSTANCE = new GeneratorManager();

    public static GeneratorManager getInstance() {
        return INSTANCE;
    }

    private volatile JobConfig currentJob;
    private volatile boolean cancelRequested = false;
    
    // Player context for progress display
    private ServerPlayerEntity jobPlayer = null;
    private String jobPlayerId = null;

    // Core engine pieces
    private ChurnWorkQueue workQueue;
    private ExecutorService workerPool;
    private MainThreadApplier applier = MainThreadApplier.getInstance();
    private ProgressLogger logger;

    // Worker pause flag (used for TPS-based throttling)
    private volatile boolean workersPaused = false;

    // Metrics
    private final AtomicLong chunksTotal = new AtomicLong(0);
    private final AtomicLong chunksCompleted = new AtomicLong(0);
    private long startTimeMillis = 0L;
    // EWMA for chunks/sec (applied)
    private double chunksPerSecEwma = 0.0;
    private long lastEwmaUpdate = System.currentTimeMillis();
    private final double cpsAlpha = 0.2; // EWMA alpha

    // checkpoint manager instance
    private RegionCheckpointManager checkpointManager = null;

    // extraction engine
    private WorldNavigator navigator = null;
    private ChunkExtractor extractor = null;
    private OutputFormatter outputFormatter = null;

    private GeneratorManager() {
    }

    public synchronized void startJob(JobConfig cfg) {
        if (currentJob != null) {
            throw new IllegalStateException("A job is already running");
        }

        // sanitize and validate config
        java.util.List<String> warns = cfg.sanitize();
        if (!warns.isEmpty()) {
            for (String w : warns) System.out.println("[Churn] job config warning: " + w);
        }
        java.util.List<String> errs = cfg.validate();
        if (!errs.isEmpty()) {
            System.err.println("[Churn] job config validation failed:");
            for (String e : errs) System.err.println("  - " + e);
            return;
        }

        this.currentJob = cfg;
        this.cancelRequested = false;

        // Compute chunk targets
        int chunkRadius = (int) Math.ceil(cfg.radius / 16.0);
        long total = (2L * chunkRadius + 1L) * (2L * chunkRadius + 1L);
        chunksTotal.set(total);
        chunksCompleted.set(0);

        // Log job start
        ConsoleLogger.jobStart(jobPlayerId != null ? jobPlayerId : "console", cfg.worldId, cfg.radius, (int)total);

        // Create queue and enqueue chunk tasks (per-chunk)
        workQueue = new ChurnWorkQueue();
        // initialize checkpoint manager
        try {
            java.nio.file.Path cp = java.nio.file.Paths.get(cfg.checkpointPath == null ? "churn_checkpoints" : cfg.checkpointPath);
            RegionCheckpointManager.init(cp);
            checkpointManager = RegionCheckpointManager.getInstance();
        } catch (Exception e) {
            System.err.println("[Churn] failed to initialize checkpoint manager: " + e + "; continuing without checkpoints");
            checkpointManager = null;
        }

        // If we have a saved queue file from a previous pause, prefer loading it
        java.nio.file.Path queueFile = java.nio.file.Paths.get(cfg.checkpointPath == null ? "churn_checkpoints" : cfg.checkpointPath).resolve("churn_last_queue.dat");
        if (java.nio.file.Files.exists(queueFile)) {
            try {
                ChurnWorkQueue loaded = ChurnWorkQueue.loadFromFile(queueFile);
                workQueue = loaded;
                System.out.println("[Churn] loaded queued tasks from " + queueFile + " (size=" + workQueue.size() + ")");
                // also restore applier queue if present
                java.nio.file.Path applierFile = java.nio.file.Paths.get(cfg.checkpointPath == null ? "churn_checkpoints" : cfg.checkpointPath).resolve("churn_last_applier.dat");
                if (java.nio.file.Files.exists(applierFile)) {
                    try {
                        int restored = applier.loadFromFile(applierFile);
                        System.out.println("[Churn] restored applier queue (" + restored + " entries) from " + applierFile);
                    } catch (Exception ex) {
                        System.err.println("[Churn] failed to restore applier queue: " + ex);
                    }
                }
            } catch (Exception ex) {
                System.err.println("[Churn] failed to load queue file: " + ex + "; falling back to full enqueue");
                // fallback to default enqueue below
            }
        }

        if (workQueue.size() == 0) {
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    // skip if checkpoint marks this chunk done
                    if (checkpointManager != null && checkpointManager.isChunkDone(dx, dz)) continue;
                    workQueue.enqueue(new ChurnTask(dx, dz));
                }
            }
        }

        // If checkpoints are present and outputPath provided, validate region files
        if (checkpointManager != null && cfg.outputPath != null) {
            try {
                java.nio.file.Path regionDir = java.nio.file.Paths.get(cfg.outputPath).resolve("region");
                int mismatches = checkpointManager.validateAgainstRegionFiles(regionDir);
                if (mismatches < 0) {
                    System.err.println("[Churn] checkpoint validation failed (I/O error)");
                } else if (mismatches > 0) {
                    String msg = "Found " + mismatches + " checkpoint(s) without matching region files in " + regionDir;
                    if (!cfg.force) {
                        System.err.println("[Churn] " + msg + ". Aborting job (use --force to override)." );
                        currentJob = null;
                        return;
                    } else {
                        System.err.println("[Churn] " + msg + ". Proceeding due to --force.");
                    }
                }
            } catch (Exception e) {
                System.err.println("[Churn] exception during checkpoint validation: " + e);
            }
        }

        // Initialize world navigator and chunk extractor
        try {
            java.nio.file.Path worldBaseDir = null;
            // First try to use the detected world directory from executeStart
            String propWorldDir = System.getProperty("churn.worldDir");
            if (propWorldDir != null) {
                worldBaseDir = java.nio.file.Paths.get(propWorldDir);
            } else {
                // Fallback: look in current directory / world
                worldBaseDir = java.nio.file.Paths.get(System.getProperty("user.dir")).resolve("world");
            }
            
            // Initialize progress logger first (before extractor that uses it)
            logger = (cfg.logPath == null) ? new ProgressLogger(chunksTotal, chunksCompleted) : new ProgressLogger(chunksTotal, chunksCompleted, cfg.logPath, cfg.logMaxBytes, cfg.logRotateCount);
            
            navigator = new WorldNavigator(cfg.worldId, worldBaseDir);
            java.util.List<String> worldIssues = navigator.validateWorld();
            if (!worldIssues.isEmpty()) {
                System.err.println("[Churn] World validation warnings:");
                for (String issue : worldIssues) System.err.println("  - " + issue);
            }
            extractor = new ChunkExtractor(navigator, logger);
            outputFormatter = new OutputFormatter(java.nio.file.Paths.get(cfg.outputPath == null ? "churn_output" : cfg.outputPath));
            System.out.println("[Churn] World navigator and extractor initialized (world base: " + worldBaseDir + ")");
        } catch (Exception e) {
            System.err.println("[Churn] failed to initialize world navigator/extractor: " + e);
            e.printStackTrace();
            currentJob = null;
            return;
        }

        // Create worker pool
        workerPool = Executors.newFixedThreadPool(Math.max(1, cfg.threads));

        // Start workers
        for (int i = 0; i < cfg.threads; i++) {
            workerPool.submit(new Worker(workQueue, applier, chunksCompleted, this, extractor));
        }

        // Start progress logger
        logger.start();

        // record start time
        startTimeMillis = System.currentTimeMillis();

        System.out.println("[Churn] Job started: " + cfg + " totalChunks=" + total);
    }
    
    /**
     * Set player context for progress display
     */
    public void setJobPlayer(ServerPlayerEntity player, String playerId) {
        this.jobPlayer = player;
        this.jobPlayerId = playerId;
    }
    
    /**
     * Get chunks total for status display
     */
    public long getChunksTotal() {
        return chunksTotal.get();
    }
    
    /**
     * Get chunks completed for status display
     */
    public long getChunksCompleted() {
        return chunksCompleted.get();
    }
    
    /**
     * Get start time for elapsed calculation
     */
    public long getStartTime() {
        return startTimeMillis;
    }

    public String getStatus() {
        if (currentJob == null) return "idle";
        long total = chunksTotal.get();
        long done = chunksCompleted.get();
        double pct = total == 0 ? 0.0 : (done * 100.0 / total);
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        return String.format("running: world=%s radius=%d threads=%d progress=%.2f%% (%d/%d) elapsed=%dms",
                currentJob.worldId, currentJob.radius, currentJob.threads, pct, done, total, elapsed);
    }

    public String getStatusJson() {
        if (currentJob == null) return "{}";
        long total = chunksTotal.get();
        long done = chunksCompleted.get();
        double pct = total == 0 ? 0.0 : (done * 100.0 / total);
        double tps = TPSMonitor.getInstance().getTps();
        boolean paused = isWorkersPaused();
        long pending = applier.pending();
        int workers = currentJob.threads;
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("world", currentJob.worldId);
        m.put("radius", currentJob.radius);
        m.put("totalChunks", total);
        m.put("doneChunks", done);
        m.put("percent", pct);
        m.put("tps", tps);
        m.put("paused", paused);
        m.put("applierPending", pending);
        m.put("workerThreads", workers);
        m.put("chunksPerSecond", getChunksPerSecond());
        double eta = -1.0;
        if (getChunksPerSecond() > 0.0001) {
            eta = (total - done) / getChunksPerSecond();
        }
        m.put("etaSeconds", eta);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (java.util.Map.Entry<String, Object> e : m.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(e.getKey()).append('"').append(':');
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) sb.append(v.toString());
            else sb.append('"').append(v.toString()).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    public synchronized void cancelCurrentJob() {
        if (currentJob == null) return;
        
        // Log cancellation
        long completed = chunksCompleted.get();
        ConsoleLogger.jobCancelled(jobPlayerId != null ? jobPlayerId : "console", (int)completed);
        
        cancelRequested = true;
        if (workerPool != null) {
            workerPool.shutdownNow();
        }
        if (logger != null) {
            logger.requestStop();
        }
        System.out.println("[Churn] cancel requested");
        
        // Clear progress display
        if (jobPlayer != null && jobPlayer.isAlive()) {
            ProgressDisplayManager.getInstance().clearProgress(jobPlayer);
        }
        
        currentJob = null;
        jobPlayer = null;
        jobPlayerId = null;
    }

    public synchronized void pauseWorkers() {
        if (workersPaused) return;
        workersPaused = true;
        System.out.println("[Churn] workers paused");
    }

    public synchronized void resumeWorkers() {
        if (!workersPaused) return;
        workersPaused = false;
        System.out.println("[Churn] workers resumed");
    }

    public boolean isWorkersPaused() {
        return workersPaused;
    }

    public synchronized void pauseCurrentJob() {
        if (currentJob == null) return;
        cancelRequested = true;
        if (workerPool != null) {
            workerPool.shutdownNow();
        }
        if (logger != null) {
            logger.requestStop();
        }

        // persist simple job state to a properties file
        try {
            java.util.Properties p = currentJob.toProperties();
            p.setProperty("chunksTotal", Long.toString(chunksTotal.get()));
            p.setProperty("chunksCompleted", Long.toString(chunksCompleted.get()));
            java.nio.file.Path out = java.nio.file.Paths.get("churn_last_job.meta");
            try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(out)) {
                p.store(os, "Churn job checkpoint");
            }
            System.out.println("[Churn] job state saved to churn_last_job.meta");
            
            // Log checkpoint
            ConsoleLogger.checkpointCreated("churn_last_job.meta", (int)chunksCompleted.get());
            
            // persist remaining queue to a file in the checkpoint directory
            try {
                java.nio.file.Path cpDir = java.nio.file.Paths.get(currentJob.checkpointPath == null ? "churn_checkpoints" : currentJob.checkpointPath);
                if (!java.nio.file.Files.exists(cpDir)) java.nio.file.Files.createDirectories(cpDir);
                java.nio.file.Path qFile = cpDir.resolve("churn_last_queue.dat");
                int count = workQueue.drainToFile(qFile);
                System.out.println("[Churn] persisted remaining queue (" + count + " entries) to " + qFile);
                // persist pending applier queue as well
                java.nio.file.Path aFile = cpDir.resolve("churn_last_applier.dat");
                int acount = applier.drainToFile(aFile);
                System.out.println("[Churn] persisted applier queue (" + acount + " entries) to " + aFile);
            } catch (Exception e) {
                System.err.println("[Churn] failed to persist queue file: " + e);
                ConsoleLogger.warn("Failed to persist queue file: %s", e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("[Churn] failed to persist job state: " + e);
            ConsoleLogger.error("Failed to persist job state: %s", e);
        }
        
        // Clear progress display
        if (jobPlayer != null && jobPlayer.isAlive()) {
            ProgressDisplayManager.getInstance().clearProgress(jobPlayer);
        }
        
        jobPlayer = null;
        jobPlayerId = null;
    }

    public synchronized void resumeJob(String path) {
        try {
            java.util.Properties p = new java.util.Properties();
            try (java.io.InputStream is = java.nio.file.Files.newInputStream(java.nio.file.Paths.get(path))) {
                p.load(is);
            }
            JobConfig cfg = JobConfig.fromProperties(p);

            // For the skeleton, resume simply starts a new job with the same config.
            System.out.println("[Churn] resuming job from: " + path + " cfg=" + cfg);
            startJob(cfg);
        } catch (Exception e) {
            System.err.println("[Churn] failed to resume job: " + e);
        }
    }

    public synchronized void cleanCheckpoints(String path) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(path == null ? "churn_checkpoints" : path);
            if (!java.nio.file.Files.exists(dir)) return;
            try (java.nio.file.DirectoryStream<java.nio.file.Path> ds = java.nio.file.Files.newDirectoryStream(dir)) {
                for (java.nio.file.Path p : ds) {
                    String n = p.getFileName().toString();
                    if (n.endsWith(".chk") || n.startsWith("churn_last_")) {
                        java.nio.file.Files.deleteIfExists(p);
                    }
                }
            }
            System.out.println("[Churn] cleaned checkpoint files in " + dir);
        } catch (Exception e) {
            System.err.println("[Churn] failed to clean checkpoints: " + e);
        }
    }

    public void tickApply() {
        JobConfig cfg = currentJob;
        if (cfg == null) return;
        double tps = TPSMonitor.getInstance().getTps();
        if (cfg.minTps > 0.0 && tps < cfg.minTps) {
            if (!isWorkersPaused()) {
                pauseWorkers();
                String msg = String.format("Throttling engaged: TPS=%.2f < minTps=%.1f", tps, cfg.minTps);
                System.out.println("[Churn] " + msg);
                ConsoleLogger.warnTPS(tps, cfg.minTps);
            }
            return;
        } else {
            if (isWorkersPaused() && tps >= cfg.minTps + cfg.tpsHysteresis) {
                resumeWorkers();
                System.out.println("[Churn] throttling released: TPS=" + String.format("%.2f", tps));
            }
        }

        int scaledBatch = Math.max(1, (int) Math.round(cfg.batch * (tps / 20.0)));
        int applied = applier.applyBatch(scaledBatch);
        if (applied > 0) {
            long now = System.currentTimeMillis();
            long dt = now - lastEwmaUpdate;
            if (dt > 0) {
                double sample = (applied * 1000.0) / dt;
                chunksPerSecEwma = cpsAlpha * sample + (1 - cpsAlpha) * chunksPerSecEwma;
                lastEwmaUpdate = now;
            }
        }
        
        // Update progress display and logging
        long completed = chunksCompleted.get();
        long total = chunksTotal.get();
        if (total > 0) {
            int percent = (int) ((completed * 100) / total);
            double speed = getChunksPerSecond();
            
            // Show hotbar progress
            if (jobPlayer != null && jobPlayer.isAlive()) {
                ProgressDisplayManager.getInstance().showProgress(jobPlayer, 
                    currentJob.worldId, percent, (int)completed, (int)total);
            }
            
            // Log progress every 10%
            if (percent % 10 == 0 && percent > 0) {
                ConsoleLogger.progress((int)completed, (int)total, speed);
            }
        }
        
        if (chunksCompleted.get() >= chunksTotal.get() && applier.pending() == 0) {
            System.out.println("[Churn] job finished: applied all chunks");
            
            // Log completion
            long elapsed = System.currentTimeMillis() - startTimeMillis;
            ConsoleLogger.jobComplete(jobPlayerId != null ? jobPlayerId : "console", 
                (int)chunksTotal.get(), elapsed);
            
            // Show completion message
            if (jobPlayer != null && jobPlayer.isAlive()) {
                ProgressDisplayManager.getInstance().completeProgress(jobPlayer, 
                    currentJob.worldId, (int)chunksTotal.get(), elapsed);
            }
            
            if (logger != null) logger.requestStop();
            if (workerPool != null) workerPool.shutdownNow();
            currentJob = null;
            jobPlayer = null;
            jobPlayerId = null;
        }
    }

    public synchronized void startPostProcess(String path) {
        try {
            java.nio.file.Path outputDir = java.nio.file.Paths.get(path == null ? "churn_output" : path);
            if (!java.nio.file.Files.exists(outputDir)) {
                System.err.println("[Churn] Output directory does not exist: " + outputDir);
                return;
            }

            // Read JSON files and re-export as different formats
            System.out.println("[Churn] Starting postprocess on: " + outputDir);
            
            // List all chunk JSON files
            java.util.List<ChunkData> allChunks = new java.util.ArrayList<>();
            try (java.nio.file.DirectoryStream<java.nio.file.Path> ds = java.nio.file.Files.newDirectoryStream(outputDir, "chunk_*.json")) {
                for (java.nio.file.Path file : ds) {
                    // Read and parse chunk JSON (simplified)
                    // In production, parse JSON properly
                    String fname = file.getFileName().toString();
                    String[] parts = fname.replace("chunk_", "").replace(".json", "").split("_");
                    if (parts.length >= 2) {
                        try {
                            int x = Integer.parseInt(parts[0]);
                            int z = Integer.parseInt(parts[1]);
                            ChunkData cd = new ChunkData();
                            cd.chunkX = x;
                            cd.chunkZ = z;
                            cd.minY = -64;
                            cd.maxY = 320;
                            cd.blockCount = 0; // read from file data
                            cd.timestamp = java.nio.file.Files.getLastModifiedTime(file).toMillis();
                            allChunks.add(cd);
                        } catch (NumberFormatException e) {
                            System.err.println("[Churn] Could not parse chunk filename: " + fname);
                        }
                    }
                }
            }

            // Export to CSV
            try {
                OutputFormatter fmt = new OutputFormatter(outputDir);
                fmt.writeCSV(allChunks);
                System.out.println("[Churn] Postprocess complete: exported " + allChunks.size() + " chunks to CSV");
            } catch (Exception e) {
                System.err.println("[Churn] Postprocess export failed: " + e);
            }
        } catch (Exception e) {
            System.err.println("[Churn] Postprocess error: " + e);
        }
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    public double getChunksPerSecond() { return chunksPerSecEwma; }
}
