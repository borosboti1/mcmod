package net.fabricmc.churn;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.churn.command.ChurnCommand;
import net.fabricmc.churn.generator.GeneratorManager;

public class ChurnMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Register commands when Fabric provides the dispatcher
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> ChurnCommand.register(dispatcher, registryAccess));

        // Register server tick to apply generated chunks on the main thread
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // record tick for TPS calculation
            net.fabricmc.churn.generator.TPSMonitor.getInstance().recordTick();
            GeneratorManager.getInstance().tickApply();
        });
    }
}
