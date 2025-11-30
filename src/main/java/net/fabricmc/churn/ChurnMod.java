package net.fabricmc.churn;

import net.fabricmc.api.ModInitializer;

public class ChurnMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Jelenleg nincs parancs regisztráció, mert a ChurnCommand nincs kifejlesztve
        System.out.println("Churn mod initialized!");
    }
}