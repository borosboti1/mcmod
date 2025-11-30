package net.fabricmc.churn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.*;

public class ChurnCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("churn")
            .then(literal("start")
                .then(argument("world", StringArgumentType.word())
                    .then(argument("radius", IntegerArgumentType.integer(16))
                        .then(argument("options", StringArgumentType.greedyString()).executes(ctx -> executeStart(ctx)))
                    )
                )
            )
            .then(literal("status")
                .then(argument("options", StringArgumentType.greedyString()).executes(ctx -> executeStatus(ctx)))
            )
            .then(literal("cancel").executes(ctx -> executeCancel(ctx)))
            .then(literal("pause").executes(ctx -> executePause(ctx)))
            .then(literal("resume")
                .then(argument("path", StringArgumentType.string()).executes(ctx -> executeResume(ctx)))
            )
            .then(literal("clean-checkpoints")
                .then(argument("path", StringArgumentType.string()).executes(ctx -> executeCleanCheckpoints(ctx)))
            )
            .then(literal("postprocess")
                .then(argument("path", StringArgumentType.string()).executes(ctx -> executePostprocess(ctx)))
            )
        );
    }

    private static int executeStart(CommandContext<ServerCommandSource> ctx) {
        // Implementáció
        return 1;
    }

    private static int executeStatus(CommandContext<ServerCommandSource> ctx) {
        // Implementáció
        return 1;
    }

    private static int executeCancel(CommandContext<ServerCommandSource> ctx) {
        // Implementáció
        return 1;
    }

    private static int executePostprocess(CommandContext<ServerCommandSource> ctx) {
        // Implementáció
        return 1;
    }

    private static int executeCleanCheckpoints(CommandContext<ServerCommandSource> ctx) {
        // Implementáció
        return 1;
    }

    private static int executePause(CommandContext<ServerCommandSource> ctx) {
        // Implementáció
        return 1;
    }

    private static int executeResume(CommandContext<ServerCommandSource> ctx) {
        // Implementáció
        return 1;
    }

    private static String safeGetString(CommandContext<ServerCommandSource> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }

    private static String safeGetStringOptional(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (Exception e) {
            return null;
        }
    }

    private static int safeGetInt(CommandContext<ServerCommandSource> ctx, String name, int def) {
        try {
            return IntegerArgumentType.getInteger(ctx, name);
        } catch (Exception e) {
            return def;
        }
    }

    private static Integer safeGetOptionalInt(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return IntegerArgumentType.getInteger(ctx, name);
        } catch (Exception e) {
            return null;
        }
    }
}