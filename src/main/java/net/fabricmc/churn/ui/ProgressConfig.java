package net.fabricmc.churn.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration for progress display and logging behavior.
 * 
 * Can be loaded from churn.properties file with these options:
 * - progress.update-interval: Milliseconds between hotbar updates (default: 3000ms)
 * - progress.bar-length: Number of characters in progress bar (default: 20)
 * - progress.show-in-actionbar: Show progress in hotbar (default: true)
 * - progress.show-chat-updates: Show progress in chat every 10% (default: true)
 * - logging.level: Console log level - DEBUG, INFO, WARN, ERROR (default: INFO)
 * - logging.color: Use colored console output (default: true)
 */
public class ProgressConfig {
    private static final ProgressConfig INSTANCE = new ProgressConfig();
    
    // Progress display settings
    public int updateInterval = 3000; // milliseconds
    public int barLength = 20;
    public boolean showInActionbar = true;
    public boolean showChatUpdates = true;
    
    // Logging settings
    public ConsoleLogger.LogLevel logLevel = ConsoleLogger.LogLevel.INFO;
    public boolean logColor = true;
    
    public static ProgressConfig getInstance() {
        return INSTANCE;
    }
    
    /**
     * Load configuration from file
     */
    public void loadFromFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                // Create default config file
                saveToFile(filePath);
                return;
            }
            
            Properties props = new Properties();
            try (var is = Files.newInputStream(path)) {
                props.load(is);
            }
            
            // Parse progress settings
            if (props.containsKey("progress.update-interval")) {
                updateInterval = Integer.parseInt(props.getProperty("progress.update-interval", "3000"));
            }
            if (props.containsKey("progress.bar-length")) {
                barLength = Integer.parseInt(props.getProperty("progress.bar-length", "20"));
            }
            if (props.containsKey("progress.show-in-actionbar")) {
                showInActionbar = Boolean.parseBoolean(props.getProperty("progress.show-in-actionbar", "true"));
            }
            if (props.containsKey("progress.show-chat-updates")) {
                showChatUpdates = Boolean.parseBoolean(props.getProperty("progress.show-chat-updates", "true"));
            }
            
            // Parse logging settings
            if (props.containsKey("logging.level")) {
                String level = props.getProperty("logging.level", "INFO").toUpperCase();
                try {
                    logLevel = ConsoleLogger.LogLevel.valueOf(level);
                } catch (IllegalArgumentException e) {
                    logLevel = ConsoleLogger.LogLevel.INFO;
                }
            }
            if (props.containsKey("logging.color")) {
                logColor = Boolean.parseBoolean(props.getProperty("logging.color", "true"));
            }
            
            ConsoleLogger.init("ProgressConfig loaded from: " + filePath);
            ConsoleLogger.init("Update interval: " + updateInterval + "ms");
            ConsoleLogger.init("Progress bar length: " + barLength);
            ConsoleLogger.init("Show actionbar: " + showInActionbar);
            ConsoleLogger.init("Show chat updates: " + showChatUpdates);
            ConsoleLogger.init("Log level: " + logLevel.toString());
            
        } catch (IOException e) {
            ConsoleLogger.warn("Failed to load config from %s: %s", filePath, e.getMessage());
        }
    }
    
    /**
     * Save current configuration to file
     */
    public void saveToFile(String filePath) {
        try {
            Properties props = new Properties();
            
            props.setProperty("progress.update-interval", String.valueOf(updateInterval));
            props.setProperty("progress.bar-length", String.valueOf(barLength));
            props.setProperty("progress.show-in-actionbar", String.valueOf(showInActionbar));
            props.setProperty("progress.show-chat-updates", String.valueOf(showChatUpdates));
            props.setProperty("logging.level", logLevel.toString());
            props.setProperty("logging.color", String.valueOf(logColor));
            
            Path path = Paths.get(filePath);
            try (var os = Files.newOutputStream(path)) {
                props.store(os, "Churn Mod Progress Display Configuration\n" +
                    "progress.update-interval: Milliseconds between hotbar updates (default: 3000)\n" +
                    "progress.bar-length: Number of characters in progress bar (default: 20)\n" +
                    "progress.show-in-actionbar: Show progress in hotbar (default: true)\n" +
                    "progress.show-chat-updates: Show progress in chat every 10% (default: true)\n" +
                    "logging.level: Console log level - DEBUG, INFO, WARN, ERROR (default: INFO)\n" +
                    "logging.color: Use colored console output (default: true)");
            }
            
            ConsoleLogger.init("ProgressConfig saved to: " + filePath);
        } catch (IOException e) {
            ConsoleLogger.error("Failed to save config to %s: %s", filePath, e.getMessage());
        }
    }
    
    /**
     * Reset to defaults
     */
    public void reset() {
        updateInterval = 3000;
        barLength = 20;
        showInActionbar = true;
        showChatUpdates = true;
        logLevel = ConsoleLogger.LogLevel.INFO;
        logColor = true;
        ConsoleLogger.init("ProgressConfig reset to defaults");
    }
}
