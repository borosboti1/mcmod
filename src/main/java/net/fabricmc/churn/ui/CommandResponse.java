package net.fabricmc.churn.ui;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Professional command response messages for the Churn Mod.
 * 
 * Provides polished, user-friendly feedback for all command operations
 * with consistent formatting and helpful information.
 */
public class CommandResponse {
    
    /**
     * Help message when command is first invoked
     */
    public static Text help() {
        return Text.literal("")
            .append(Text.literal("=== Churn Mod v1.1 ===\n").formatted(Formatting.GOLD))
            .append(Text.literal("/churn help").formatted(Formatting.YELLOW))
            .append(Text.literal(" - Command reference\n").formatted(Formatting.DARK_GRAY))
            .append(Text.literal("/churn world <name>").formatted(Formatting.YELLOW))
            .append(Text.literal(" - Set target dimension\n").formatted(Formatting.DARK_GRAY))
            .append(Text.literal("/churn radius <value>").formatted(Formatting.YELLOW))
            .append(Text.literal(" - Set scan radius\n").formatted(Formatting.DARK_GRAY))
            .append(Text.literal("/churn settings").formatted(Formatting.YELLOW))
            .append(Text.literal(" - View configuration\n").formatted(Formatting.DARK_GRAY))
            .append(Text.literal("/churn start").formatted(Formatting.YELLOW))
            .append(Text.literal(" - Begin extraction").formatted(Formatting.DARK_GRAY));
    }
    
    /**
     * Message when extraction starts
     */
    public static Text extractionStarted(String dimension, int radius, int totalChunks) {
        return Text.literal("")
            .append(Text.literal("[Churn] ").formatted(Formatting.GREEN))
            .append(Text.literal("Starting chunk extraction...\n").formatted(Formatting.WHITE))
            .append(Text.literal("• Dimension: ").formatted(Formatting.GRAY))
            .append(Text.literal(dimension + "\n").formatted(Formatting.YELLOW))
            .append(Text.literal("• Radius: ").formatted(Formatting.GRAY))
            .append(Text.literal(radius + " chunks\n").formatted(Formatting.YELLOW))
            .append(Text.literal("• Estimated chunks: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%,d", totalChunks) + "\n").formatted(Formatting.YELLOW))
            .append(Text.literal("• Progress will display above your hotbar").formatted(Formatting.DARK_GRAY));
    }
    
    /**
     * Status message showing current extraction progress
     */
    public static Text status(int percent, int current, int total, long elapsedMillis, 
                              double chunksPerSec, long estimatedRemainingMillis) {
        String progressBar = buildProgressBar(percent, 10);
        long elapsedSeconds = elapsedMillis / 1000;
        long remainingSeconds = estimatedRemainingMillis / 1000;
        
        return Text.literal("")
            .append(Text.literal("[Churn] Extraction Status:\n").formatted(Formatting.GOLD))
            .append(Text.literal("• Progress: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%d%%", percent)).formatted(Formatting.GREEN))
            .append(Text.literal(" ").formatted(Formatting.RESET))
            .append(Text.literal(progressBar).formatted(Formatting.GREEN))
            .append(Text.literal("\n• Chunks: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%,d/%,d", current, total)).formatted(Formatting.AQUA))
            .append(Text.literal(" processed\n").formatted(Formatting.GRAY))
            .append(Text.literal("• Time elapsed: ").formatted(Formatting.GRAY))
            .append(Text.literal(formatTime(elapsedSeconds) + "\n").formatted(Formatting.YELLOW))
            .append(Text.literal("• Time remaining: ").formatted(Formatting.GRAY))
            .append(Text.literal(formatTime(remainingSeconds) + "\n").formatted(Formatting.YELLOW))
            .append(Text.literal("• Speed: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.1f chunks/sec", chunksPerSec)).formatted(Formatting.LIGHT_PURPLE));
    }
    
    /**
     * Message when extraction is cancelled
     */
    public static Text extractionCancelled(int chunksProcessed, String partialPath) {
        return Text.literal("")
            .append(Text.literal("[Churn] Extraction cancelled\n").formatted(Formatting.RED))
            .append(Text.literal("• Processed: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%,d chunks", chunksProcessed) + "\n").formatted(Formatting.YELLOW))
            .append(Text.literal("• Partial data saved to: ").formatted(Formatting.GRAY))
            .append(Text.literal(partialPath).formatted(Formatting.WHITE));
    }
    
    /**
     * Message when extraction is paused
     */
    public static Text extractionPaused(String checkpointPath) {
        return Text.literal("")
            .append(Text.literal("[Churn] Extraction paused\n").formatted(Formatting.YELLOW))
            .append(Text.literal("• Checkpoint saved\n").formatted(Formatting.GRAY))
            .append(Text.literal("• Resume with: ").formatted(Formatting.GRAY))
            .append(Text.literal("/churn resume \"" + checkpointPath + "\"").formatted(Formatting.WHITE));
    }
    
    /**
     * Message when extraction completes successfully
     */
    public static Text extractionComplete(int totalChunks, long elapsedMillis, String outputPath) {
        double seconds = elapsedMillis / 1000.0;
        double speed = totalChunks / Math.max(seconds, 1.0);
        
        return Text.literal("")
            .append(Text.literal("[Churn] Extraction complete! ✓\n").formatted(Formatting.GREEN))
            .append(Text.literal("• Processed: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%,d chunks", totalChunks)).formatted(Formatting.YELLOW))
            .append(Text.literal(" in ").formatted(Formatting.GRAY))
            .append(Text.literal(formatTime(elapsedMillis / 1000) + "\n").formatted(Formatting.YELLOW))
            .append(Text.literal("• Average speed: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.1f chunks/sec", speed) + "\n").formatted(Formatting.LIGHT_PURPLE))
            .append(Text.literal("• Data saved to: ").formatted(Formatting.GRAY))
            .append(Text.literal(outputPath).formatted(Formatting.WHITE));
    }
    
    /**
     * Generic error message
     */
    public static Text error(String title, String reason, String suggestion) {
        return Text.literal("")
            .append(Text.literal("[Churn] Error: " + title + "\n").formatted(Formatting.RED))
            .append(Text.literal("• Reason: ").formatted(Formatting.GRAY))
            .append(Text.literal(reason + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("• Suggestion: ").formatted(Formatting.GRAY))
            .append(Text.literal(suggestion).formatted(Formatting.WHITE));
    }
    
    /**
     * World not found error
     */
    public static Text errorWorldNotFound(String worldName, String[] availableWorlds) {
        StringBuilder worlds = new StringBuilder();
        for (int i = 0; i < availableWorlds.length; i++) {
            if (i > 0) worlds.append(", ");
            worlds.append(availableWorlds[i]);
        }
        
        return Text.literal("")
            .append(Text.literal("[Churn] Error: World '" + worldName + "' not found\n").formatted(Formatting.RED))
            .append(Text.literal("• Available dimensions: ").formatted(Formatting.GRAY))
            .append(Text.literal(worlds.toString() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("• Use: ").formatted(Formatting.GRAY))
            .append(Text.literal("/churn world <name>").formatted(Formatting.WHITE));
    }
    
    /**
     * Already running error
     */
    public static Text errorAlreadyRunning() {
        return Text.literal("")
            .append(Text.literal("[Churn] Error: Extraction already running\n").formatted(Formatting.RED))
            .append(Text.literal("• Cancel current job: ").formatted(Formatting.GRAY))
            .append(Text.literal("/churn cancel").formatted(Formatting.WHITE))
            .append(Text.literal(" or ").formatted(Formatting.GRAY))
            .append(Text.literal("pause with /churn pause").formatted(Formatting.WHITE));
    }
    
    /**
     * No job running error
     */
    public static Text errorNoJobRunning() {
        return Text.literal("")
            .append(Text.literal("[Churn] Error: No extraction job currently running\n").formatted(Formatting.RED))
            .append(Text.literal("• Start a new job: ").formatted(Formatting.GRAY))
            .append(Text.literal("/churn start").formatted(Formatting.WHITE));
    }
    
    /**
     * Success confirmation for settings
     */
    public static Text settingChanged(String key, String value) {
        return Text.literal("")
            .append(Text.literal("[Churn] ").formatted(Formatting.GREEN))
            .append(Text.literal(key).formatted(Formatting.YELLOW))
            .append(Text.literal(" set to ").formatted(Formatting.WHITE))
            .append(Text.literal(value).formatted(Formatting.YELLOW));
    }
    
    /**
     * Settings reset confirmation
     */
    public static Text settingsReset() {
        return Text.literal("")
            .append(Text.literal("[Churn] ").formatted(Formatting.GREEN))
            .append(Text.literal("All settings reset to defaults").formatted(Formatting.WHITE));
    }
    
    /**
     * Build progress bar: [||||••••]
     */
    private static String buildProgressBar(int percent, int length) {
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        
        int filled = (percent * length) / 100;
        int empty = length - filled;
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < filled; i++) {
            sb.append("|");
        }
        for (int i = 0; i < empty; i++) {
            sb.append("•");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Format seconds to readable time string
     */
    private static String formatTime(long seconds) {
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
