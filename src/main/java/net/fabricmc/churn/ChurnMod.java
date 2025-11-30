package net.fabricmc.churn;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ChurnMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Új Fabric API parancs regisztráció
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ChurnCommand.register(dispatcher);
        });
    }
}