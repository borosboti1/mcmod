package net.fabricmc.churn;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.churn.ui.ConsoleLogger;
import net.fabricmc.churn.ui.ProgressConfig;
import net.fabricmc.churn.ui.ProgressDisplayManager;
import net.fabricmc.churn.generator.GeneratorManager;

public class ChurnMod implements ModInitializer {
    public static final String MOD_ID = "churn";
    
    @Override
    public void onInitialize() {
        // Initialize with new logging system
        ConsoleLogger.init("Churn Mod v0.2.1 initializing");
        
        // Load configuration
        try {
            ProgressConfig.getInstance().loadFromFile("churn.properties");
            ConsoleLogger.init("[CONFIG] Progress settings loaded");
        } catch (Exception e) {
            ConsoleLogger.warn("Failed to load configuration: %s", e.getMessage());
        }
        
        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            try {
                ConsoleLogger.init("[CMD] Registering Churn commands...");
                net.fabricmc.churn.command.ChurnCommand.register(dispatcher);
                ConsoleLogger.init("[CMD] Commands registered successfully");
            } catch (Exception e) {
                ConsoleLogger.error("Failed to register commands: %s", e);
            }
        });

        // Register server tick handler to apply main-thread applier work each tick
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            try {
                GeneratorManager.getInstance().tickApply();
            } catch (Exception e) {
                ConsoleLogger.error("Tick apply error: %s", e);
            }
        });

        // Clean up on server stopping
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                GeneratorManager.getInstance().cancelCurrentJob();
            } catch (Exception e) {
                // ignore
            }
            try {
                ProgressDisplayManager.getInstance().shutdown();
            } catch (Exception e) {
                // ignore
            }
        });
        
        ConsoleLogger.init("[INIT] Churn Mod initialization complete");
    }
}