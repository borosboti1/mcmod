package net.fabricmc.churn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class ChurnCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        System.out.println("ChurnCommand.register() ELKEDZŐDIK");
        
        dispatcher.register(
            literal("churntest")
                .executes(context -> {
                    context.getSource().sendMessage(Text.literal("§aChurn teszt parancs MŰKÖDIK!"));
                    System.out.println("Churn teszt parancs végrehajtva");
                    return 1;
                })
        );
        
        System.out.println("ChurnCommand.register() BEFEJEZVE");
    }
}