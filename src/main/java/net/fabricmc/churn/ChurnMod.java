package net.fabricmc.churn;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChurnMod implements ModInitializer {
    public static final String MOD_ID = "churn";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    @Override
    public void onInitialize() {
        LOGGER.info("=== CHURN MOD INICIALIZÁLÁS ===");
        LOGGER.info("1. Mod main class betöltve");
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LOGGER.info("2. CommandRegistrationCallback meghívva");
            LOGGER.info("3. Environment: " + environment.toString());
            LOGGER.info("4. Dispatcher: " + dispatcher.toString());
            
            try {
                net.fabricmc.churn.command.ChurnCommand.register(dispatcher);
                LOGGER.info("5. ChurnCommand.register() sikeres");
            } catch (Exception e) {
                LOGGER.error("6. Hiba a parancs regisztrációban: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        LOGGER.info("=== CHURN MOD INICIALIZÁLÁS BEFEJEZVE ===");
    }
}