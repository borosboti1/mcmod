package net.fabricmc.churn.ui;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages real-time progress display in player's action bar (above hotbar).
 * 
 * Features:
 * - Animated progress bar with filled/unfilled symbols
 * - Displays percentage and chunk counter
 * - Updates every 2-5 seconds (configurable)
 * - Supports multiple concurrent players
 * - Auto-cleanup when jobs complete
 */
public class ProgressDisplayManager {
    private static final ProgressDisplayManager INSTANCE = new ProgressDisplayManager();
    
    private Map<UUID, ProgressDisplay> activeDisplays = Collections.synchronizedMap(new HashMap<>());
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "Churn-ProgressDisplay");
        t.setDaemon(true);
        return t;
    });
    
    private volatile boolean running = true;
    
    public static ProgressDisplayManager getInstance() {
        return INSTANCE;
    }
    
    private ProgressDisplayManager() {
        // Load configuration
        ProgressConfig config = ProgressConfig.getInstance();
        
        // Start background update task with configurable interval
        scheduler.scheduleAtFixedRate(this::updateAllDisplays, 1, config.updateInterval, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Show progress for a player's extraction job
     */
    public void showProgress(ServerPlayerEntity player, String jobId, 
                            int percentage, int current, int total) {
        if (player == null || !running) return;
        
        UUID playerId = player.getUuid();
        
        // Create or update display for this player
        ProgressDisplay display = activeDisplays.computeIfAbsent(playerId, k -> 
            new ProgressDisplay(player, jobId)
        );
        
        // Update progress data
        display.percentage = percentage;
        display.currentChunks = current;
        display.totalChunks = total;
        display.lastUpdate = System.currentTimeMillis();
        display.needsUpdate = true;
    }
    
    /**
     * Clear progress display for a player
     */
    public void clearProgress(ServerPlayerEntity player) {
        if (player == null) return;
        activeDisplays.remove(player.getUuid());
    }
    
    /**
     * Complete a job and show completion message
     */
    public void completeProgress(ServerPlayerEntity player, String jobId, 
                                 int totalChunks, long elapsedMillis) {
        if (player == null) return;
        
        double elapsedSeconds = elapsedMillis / 1000.0;
        double chunksPerSec = totalChunks / Math.max(elapsedSeconds, 1.0);
        
        Text message = Text.literal("")
            .append(Text.literal("[Churn] ").formatted(Formatting.GOLD))
            .append(Text.literal("Extraction complete! ✓\n").formatted(Formatting.GREEN))
            .append(Text.literal("• Processed: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%,d chunks", totalChunks)).formatted(Formatting.YELLOW))
            .append(Text.literal(" in ").formatted(Formatting.GRAY))
            .append(Text.literal(formatTime(elapsedMillis)).formatted(Formatting.YELLOW))
            .append(Text.literal(" at ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.1f chunks/sec", chunksPerSec)).formatted(Formatting.LIGHT_PURPLE));
        
        player.sendMessage(message, false);
        clearProgress(player);
    }
    
    /**
     * Background task: update all active displays
     */
    private void updateAllDisplays() {
        if (!running) return;
        
        List<UUID> toRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, ProgressDisplay> entry : activeDisplays.entrySet()) {
            ProgressDisplay display = entry.getValue();
            
            // Check if player is still online
            if (!display.player.isAlive()) {
                toRemove.add(entry.getKey());
                continue;
            }
            
            // Send update if needed
            if (display.needsUpdate) {
                String progressBar = buildProgressBar(display.percentage, 20);
                
                Text message = Text.literal("")
                    .append(Text.literal("[Churn] ").formatted(Formatting.GOLD))
                    .append(Text.literal("Extracting... ").formatted(Formatting.YELLOW))
                    .append(Text.literal(progressBar).formatted(Formatting.GREEN))
                    .append(Text.literal(" ").formatted(Formatting.RESET))
                    .append(Text.literal(display.percentage + "%").formatted(Formatting.AQUA))
                    .append(Text.literal(" (").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(String.format("%d/%d chunks", display.currentChunks, display.totalChunks))
                        .formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(")").formatted(Formatting.DARK_GRAY));
                
                // Send to action bar (second parameter = true for actionbar)
                display.player.sendMessage(message, true);
                display.needsUpdate = false;
            }
            
            // Auto-remove if idle for 30 seconds
            if (System.currentTimeMillis() - display.lastUpdate > 30000) {
                toRemove.add(entry.getKey());
            }
        }
        
        // Clean up offline players
        for (UUID id : toRemove) {
            activeDisplays.remove(id);
        }
    }
    
    /**
     * Build progress bar string: [||||••••]
     */
    private String buildProgressBar(int percent, int length) {
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        
        // Use configured bar length if available
        ProgressConfig config = ProgressConfig.getInstance();
        int barLen = config.barLength;
        
        int filled = (percent * barLen) / 100;
        int empty = barLen - filled;
        
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
     * Format milliseconds to readable time string
     */
    private String formatTime(long millis) {
        long seconds = millis / 1000;
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
    
    /**
     * Shutdown the display manager (on server stop)
     */
    public void shutdown() {
        running = false;
        activeDisplays.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get active display count (for debugging)
     */
    public int getActiveDisplayCount() {
        return activeDisplays.size();
    }
    
    /**
     * Inner class representing a player's progress display
     */
    private static class ProgressDisplay {
        ServerPlayerEntity player;
        String jobId;
        int percentage = 0;
        int currentChunks = 0;
        int totalChunks = 0;
        long lastUpdate = System.currentTimeMillis();
        boolean needsUpdate = false;
        
        ProgressDisplay(ServerPlayerEntity player, String jobId) {
            this.player = player;
            this.jobId = jobId;
        }
    }
}
