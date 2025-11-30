package net.fabricmc.churn.generator;

public class JobConfig {
    public String worldId;
    public int radius = 512;
    public int threads = 4;
    public String outputPath;
    public boolean skipLighting = false;
    public boolean skipEntities = false;
    public boolean fastGenerate = false;
    public int batch = 2;
    public double minTps = 0.0;
    public double tpsHysteresis = 0.5;
    public String logPath;
    public long logMaxBytes = 10_000_000L;
    public int logRotateCount = 3;
    public String checkpointPath;
    public boolean force = false;

    public java.util.Properties toProperties() {
        java.util.Properties p = new java.util.Properties();
        p.setProperty("worldId", worldId == null ? "" : worldId);
        p.setProperty("radius", Integer.toString(radius));
        p.setProperty("threads", Integer.toString(threads));
        if (outputPath != null) p.setProperty("outputPath", outputPath);
        p.setProperty("skipLighting", Boolean.toString(skipLighting));
        p.setProperty("skipEntities", Boolean.toString(skipEntities));
        p.setProperty("fastGenerate", Boolean.toString(fastGenerate));
        p.setProperty("batch", Integer.toString(batch));
        p.setProperty("minTps", Double.toString(minTps));
        p.setProperty("tpsHysteresis", Double.toString(tpsHysteresis));
        if (logPath != null) p.setProperty("logPath", logPath);
        p.setProperty("logMaxBytes", Long.toString(logMaxBytes));
        p.setProperty("logRotateCount", Integer.toString(logRotateCount));
        if (checkpointPath != null) p.setProperty("checkpointPath", checkpointPath);
        p.setProperty("force", Boolean.toString(force));
        return p;
    }

    public static JobConfig fromProperties(java.util.Properties p) {
        JobConfig cfg = new JobConfig();
        cfg.worldId = p.getProperty("worldId", "minecraft:overworld");
        cfg.radius = Integer.parseInt(p.getProperty("radius", "512"));
        cfg.threads = Integer.parseInt(p.getProperty("threads", "4"));
        cfg.outputPath = p.getProperty("outputPath");
        cfg.skipLighting = Boolean.parseBoolean(p.getProperty("skipLighting", "false"));
        cfg.skipEntities = Boolean.parseBoolean(p.getProperty("skipEntities", "false"));
        cfg.fastGenerate = Boolean.parseBoolean(p.getProperty("fastGenerate", "false"));
        cfg.batch = Integer.parseInt(p.getProperty("batch", "2"));
        cfg.minTps = Double.parseDouble(p.getProperty("minTps", "0.0"));
        cfg.tpsHysteresis = Double.parseDouble(p.getProperty("tpsHysteresis", "0.5"));
        cfg.logPath = p.getProperty("logPath");
        cfg.logMaxBytes = Long.parseLong(p.getProperty("logMaxBytes", "10000000"));
        cfg.logRotateCount = Integer.parseInt(p.getProperty("logRotateCount", "3"));
        cfg.checkpointPath = p.getProperty("checkpointPath");
        cfg.force = Boolean.parseBoolean(p.getProperty("force", "false"));
        return cfg;
    }

    @Override
    public String toString() {
        return "JobConfig[world=" + worldId + ", radius=" + radius + ", threads=" + threads + "]";
    }
}
