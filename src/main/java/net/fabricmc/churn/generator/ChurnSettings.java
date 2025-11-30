package net.fabricmc.churn.generator;

import java.util.*;

/**
 * Per-user settings manager for Churn command state.
 * Stores settings between commands for easier workflow.
 */
public class ChurnSettings {
    private static final Map<String, ChurnSettings> USER_SETTINGS = new HashMap<>();

    private String worldId = "overworld";
    private int radius = 10;
    private int threads = 4;
    private String outputPath = "churn_output";
    private String checkpointPath = "churn_checkpoints";
    private double minTps = 15.0;
    private boolean verbose = false;
    private String outputFormat = "json"; // json or csv
    private boolean fastMode = false;

    private ChurnSettings() {
    }

    /**
     * Get or create settings for a player.
     */
    public static synchronized ChurnSettings getSettings(String playerId) {
        return USER_SETTINGS.computeIfAbsent(playerId, k -> new ChurnSettings());
    }

    /**
     * Create settings from JobConfig (for compatibility).
     */
    public static ChurnSettings fromJobConfig(JobConfig cfg) {
        ChurnSettings s = new ChurnSettings();
        s.worldId = cfg.worldId;
        s.radius = (int) cfg.radius;
        s.threads = cfg.threads;
        s.outputPath = cfg.outputPath;
        s.checkpointPath = cfg.checkpointPath;
        s.minTps = cfg.minTps;
        return s;
    }

    /**
     * Convert to JobConfig for execution.
     */
    public JobConfig toJobConfig() {
        JobConfig cfg = new JobConfig();
        cfg.worldId = this.worldId;
        cfg.radius = this.radius;
        cfg.threads = this.threads;
        cfg.outputPath = this.outputPath;
        cfg.checkpointPath = this.checkpointPath;
        cfg.minTps = this.minTps;
        return cfg;
    }

    // Getters and Setters
    public String getWorldId() { return worldId; }
    public void setWorldId(String worldId) { this.worldId = worldId; }

    public int getRadius() { return radius; }
    public void setRadius(int radius) { this.radius = Math.max(1, Math.min(radius, 2000)); }

    public int getThreads() { return threads; }
    public void setThreads(int threads) { this.threads = Math.max(1, Math.min(threads, 32)); }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String path) { this.outputPath = path; }

    public String getCheckpointPath() { return checkpointPath; }
    public void setCheckpointPath(String path) { this.checkpointPath = path; }

    public double getMinTps() { return minTps; }
    public void setMinTps(double tps) { this.minTps = Math.max(0.0, Math.min(tps, 20.0)); }

    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean v) { this.verbose = v; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String format) { 
        if (format.equals("json") || format.equals("csv")) this.outputFormat = format;
    }

    public boolean isFastMode() { return fastMode; }
    public void setFastMode(boolean fast) { this.fastMode = fast; }

    /**
     * Get summary of current settings.
     */
    public String getSummary() {
        return String.format(
            "World: %s | Radius: %d | Threads: %d | Output: %s | MinTPS: %.1f | Format: %s%s",
            worldId, radius, threads, outputPath, minTps, outputFormat,
            fastMode ? " | FastMode: ON" : ""
        );
    }

    /**
     * Reset to defaults.
     */
    public void reset() {
        this.worldId = "overworld";
        this.radius = 10;
        this.threads = 4;
        this.outputPath = "churn_output";
        this.checkpointPath = "churn_checkpoints";
        this.minTps = 15.0;
        this.verbose = false;
        this.outputFormat = "json";
        this.fastMode = false;
    }

    @Override
    public String toString() {
        return "ChurnSettings{" +
                "world='" + worldId + '\'' +
                ", radius=" + radius +
                ", threads=" + threads +
                ", output='" + outputPath + '\'' +
                ", minTps=" + minTps +
                ", verbose=" + verbose +
                ", format='" + outputFormat + '\'' +
                ", fastMode=" + fastMode +
                '}';
    }
}
