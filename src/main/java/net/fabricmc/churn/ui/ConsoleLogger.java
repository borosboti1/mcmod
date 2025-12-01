package net.fabricmc.churn.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Structured console logging for Churn Mod operations.
 * 
 * Log categories:
 * - [INIT] - Cyan - Initialization messages
 * - [JOB] - Green - Job start/stop events
 * - [PROGRESS] - Blue - Progress updates (every 10%)
 * - [SAVE] - Purple - File operations
 * - [WARN] - Yellow - Non-critical issues
 * - [ERROR] - Red - Critical failures
 */
public class ConsoleLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("Churn");
    
    // Log level configuration
    private static volatile LogLevel minLogLevel = LogLevel.INFO;
    
    public enum LogLevel {
        DEBUG(0), INFO(1), WARN(2), ERROR(3);
        
        final int level;
        LogLevel(int level) {
            this.level = level;
        }
    }
    
    /**
     * Set minimum log level
     */
    public static void setLogLevel(LogLevel level) {
        minLogLevel = level;
    }
    
    /**
     * Log initialization event
     */
    public static void init(String message) {
        if (minLogLevel.level <= LogLevel.INFO.level) {
            LOGGER.info("[INIT] {}", message);
        }
    }
    
    /**
     * Log job start
     */
    public static void jobStart(String playerName, String dimension, int radius, int estimatedChunks) {
        if (minLogLevel.level <= LogLevel.INFO.level) {
            LOGGER.info("[JOB] Player '{}' started extraction: {} (radius: {} chunks, estimated: {})",
                    playerName, dimension, radius, String.format("%,d", estimatedChunks));
        }
    }
    
    /**
     * Log job completion
     */
    public static void jobComplete(String playerName, int totalChunks, long elapsedMillis) {
        if (minLogLevel.level <= LogLevel.INFO.level) {
            double seconds = elapsedMillis / 1000.0;
            double speed = totalChunks / Math.max(seconds, 1.0);
            LOGGER.info("[JOB] Extraction finished for player '{}': {} chunks in {}s (avg {:.1f} chunks/sec)",
                    playerName, String.format("%,d", totalChunks), 
                    (long) seconds, speed);
        }
    }
    
    /**
     * Log job cancellation
     */
    public static void jobCancelled(String playerName, int chunksProcessed) {
        if (minLogLevel.level <= LogLevel.INFO.level) {
            LOGGER.info("[JOB] Extraction cancelled by player '{}': {} chunks processed",
                    playerName, String.format("%,d", chunksProcessed));
        }
    }
    
    /**
     * Log progress checkpoint (every 10%)
     */
    public static void progress(int current, int total, double chunksPerSec) {
        if (minLogLevel.level <= LogLevel.INFO.level) {
            int percent = (int) ((current * 100.0) / total);
            LOGGER.info("[PROGRESS] {}/{} chunks ({}%) - {:.1f} chunks/sec",
                    String.format("%,d", current), String.format("%,d", total),
                    percent, chunksPerSec);
        }
    }
    
    /**
     * Log detailed progress update (for more verbose tracking)
     */
    public static void progressDetailed(int current, int total, double chunksPerSec, long elapsedMillis) {
        if (minLogLevel.level <= LogLevel.DEBUG.level) {
            int percent = (int) ((current * 100.0) / total);
            long secondsElapsed = elapsedMillis / 1000;
            long secondsRemaining = (long) ((total - current) / Math.max(chunksPerSec, 0.1));
            
            LOGGER.debug("[PROGRESS] {}/{} ({}%) - {:.1f}/sec - Elapsed: {}s, Est. remaining: {}s",
                    String.format("%,d", current), String.format("%,d", total), percent,
                    chunksPerSec, secondsElapsed, secondsRemaining);
        }
    }
    
    /**
     * Log file save operation
     */
    public static void fileSave(String fileName, int itemCount) {
        if (minLogLevel.level <= LogLevel.INFO.level) {
            LOGGER.info("[SAVE] Saved {} items to: {}", String.format("%,d", itemCount), fileName);
        }
    }
    
    /**
     * Log checkpoint creation
     */
    public static void checkpointCreated(String checkpointPath, int chunksProcessed) {
        if (minLogLevel.level <= LogLevel.INFO.level) {
            LOGGER.info("[SAVE] Checkpoint created: {} (progress: {} chunks)",
                    checkpointPath, String.format("%,d", chunksProcessed));
        }
    }
    
    /**
     * Log checkpoint loaded
     */
    public static void checkpointLoaded(String checkpointPath, int resumeFrom) {
        if (minLogLevel.level <= LogLevel.INFO.level) {
            LOGGER.info("[SAVE] Checkpoint loaded: {} (resuming from {} chunks)",
                    checkpointPath, String.format("%,d", resumeFrom));
        }
    }
    
    /**
     * Log non-critical warning
     */
    public static void warn(String message, Object... args) {
        if (minLogLevel.level <= LogLevel.WARN.level) {
            LOGGER.warn("[WARN] {}", String.format(message, args));
        }
    }
    
    /**
     * Log TPS warning
     */
    public static void warnTPS(double currentTPS, double minimumTPS) {
        if (minLogLevel.level <= LogLevel.WARN.level) {
            LOGGER.warn("[WARN] Server TPS dropped to {:.1f} (minimum configured: {:.1f}), throttling extraction",
                    currentTPS, minimumTPS);
        }
    }
    
    /**
     * Log corrupted region file warning
     */
    public static void warnCorruptedRegion(String regionFileName, String reason) {
        if (minLogLevel.level <= LogLevel.WARN.level) {
            LOGGER.warn("[WARN] Corrupted or unreadable region file: {} (reason: {})",
                    regionFileName, reason);
        }
    }
    
    /**
     * Log error condition
     */
    public static void error(String message, Object... args) {
        if (minLogLevel.level <= LogLevel.ERROR.level) {
            LOGGER.error("[ERROR] {}", String.format(message, args));
        }
    }
    
    /**
     * Log error with exception
     */
    public static void error(String message, Throwable exception, Object... args) {
        if (minLogLevel.level <= LogLevel.ERROR.level) {
            LOGGER.error("[ERROR] " + String.format(message, args), exception);
        }
    }
    
    /**
     * Log failed chunk extraction
     */
    public static void errorChunkExtraction(int chunkX, int chunkZ, String reason) {
        if (minLogLevel.level <= LogLevel.WARN.level) {
            LOGGER.warn("[WARN] Failed to extract chunk {},{}  (reason: {})",
                    chunkX, chunkZ, reason);
        }
    }
    
    /**
     * Log critical error
     */
    public static void errorCritical(String message, Throwable exception) {
        if (minLogLevel.level <= LogLevel.ERROR.level) {
            LOGGER.error("[ERROR] CRITICAL: {}", message, exception);
        }
    }
    
    /**
     * Log configuration info
     */
    public static void config(String key, String value) {
        if (minLogLevel.level <= LogLevel.DEBUG.level) {
            LOGGER.debug("[CONFIG] {} = {}", key, value);
        }
    }
}
