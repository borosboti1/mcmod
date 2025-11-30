package net.fabricmc.churn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import net.fabricmc.churn.generator.GeneratorManager;
import net.fabricmc.churn.generator.JobConfig;

public class ChurnCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
            literal("churn")
                .then(literal("start")
                    .then(argument("world", StringArgumentType.word())
                        .then(argument("radius", IntegerArgumentType.integer(16))
                            .then(argument("options", greedyString())
                                .executes(ctx -> executeStart(ctx))
                            )
                            .executes(ctx -> executeStart(ctx))
                        )
                    )
                )
                .then(literal("status")
                    .then(argument("options", greedyString()).executes(ctx -> executeStatus(ctx)))
                    .executes(ctx -> executeStatus(ctx))
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
        String world = safeGetString(ctx, "world");
        int radius = safeGetInt(ctx, "radius", 512);
        String options = safeGetStringOptional(ctx, "options");

        JobConfig cfg = new JobConfig();
        cfg.worldId = world;
        cfg.radius = radius;
        // parse options string for flags like --threads, --no_lighting, --output, etc.
        if (options != null && !options.isEmpty()) {
            parseOptionsIntoConfig(options, cfg);
        }

        GeneratorManager.getInstance().startJob(cfg);
        ctx.getSource().sendFeedback(() -> "Churn: started job for world=" + world + " radius=" + radius + " threads=" + cfg.threads, false);
        return 1;
    }

    private static int executeStatus(CommandContext<ServerCommandSource> ctx) {
        String options = safeGetStringOptional(ctx, "options");
        String status = GeneratorManager.getInstance().getStatus();
        if (options != null && options.contains("--json")) {
            String json = GeneratorManager.getInstance().getStatusJson();
            ctx.getSource().sendFeedback(() -> json, false);
        } else {
            ctx.getSource().sendFeedback(() -> "Churn status: " + status, false);
        }
        return 1;
    }

    private static int executeCancel(CommandContext<ServerCommandSource> ctx) {
        GeneratorManager.getInstance().cancelCurrentJob();
        ctx.getSource().sendFeedback(() -> "Churn: cancel requested", false);
        return 1;
    }

    private static int executePostprocess(CommandContext<ServerCommandSource> ctx) {
        String path = safeGetString(ctx, "path");
        GeneratorManager.getInstance().startPostProcess(path);
        ctx.getSource().sendFeedback(() -> "Churn: postprocess started for " + path, false);
        return 1;
    }

    private static int executeCleanCheckpoints(CommandContext<ServerCommandSource> ctx) {
        String path = safeGetString(ctx, "path");
        GeneratorManager.getInstance().cleanCheckpoints(path);
        ctx.getSource().sendFeedback(() -> "Churn: cleaned checkpoints in " + path, false);
        return 1;
    }

    private static int executePause(CommandContext<ServerCommandSource> ctx) {
        GeneratorManager.getInstance().pauseCurrentJob();
        ctx.getSource().sendFeedback(() -> "Churn: pause requested", false);
        return 1;
    }

    private static int executeResume(CommandContext<ServerCommandSource> ctx) {
        String path = safeGetString(ctx, "path");
        GeneratorManager.getInstance().resumeJob(path);
        ctx.getSource().sendFeedback(() -> "Churn: resume requested from " + path, false);
        return 1;
    }

    private static String safeGetString(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (IllegalArgumentException ex) {
            return "minecraft:overworld";
        }
    }

    private static String safeGetStringOptional(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static int safeGetInt(CommandContext<ServerCommandSource> ctx, String name, int def) {
        try {
            return IntegerArgumentType.getInteger(ctx, name);
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    private static Integer safeGetOptionalInt(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return IntegerArgumentType.getInteger(ctx, name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void parseOptionsIntoConfig(String options, JobConfig cfg) {
        String[] parts = options.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            switch (p) {
                case "--no_lighting":
                case "--skip_lighting":
                    cfg.skipLighting = true;
                    break;
                case "--no_entities":
                case "--skip_entities":
                    cfg.skipEntities = true;
                    break;
                case "--fast_generate":
                    cfg.fastGenerate = true;
                    break;
                case "--output":
                    if (i + 1 < parts.length) {
                        cfg.outputPath = parts[++i];
                    }
                    break;
                case "--threads":
                    if (i + 1 < parts.length) {
                        try { cfg.threads = Integer.parseInt(parts[++i]); } catch (NumberFormatException e) {}
                    }
                    break;
                case "--batch":
                    if (i + 1 < parts.length) {
                        try { cfg.batch = Integer.parseInt(parts[++i]); } catch (NumberFormatException e) {}
                    }
                    break;
                case "--min_tps":
                    if (i + 1 < parts.length) {
                        try { cfg.minTps = Double.parseDouble(parts[++i]); } catch (NumberFormatException e) {}
                    }
                    break;
                case "--log":
                    if (i + 1 < parts.length) {
                        cfg.logPath = parts[++i];
                    }
                    break;
                case "--log_max_bytes":
                    if (i + 1 < parts.length) {
                        try { cfg.logMaxBytes = Long.parseLong(parts[++i]); } catch (NumberFormatException e) {}
                    }
                    break;
                case "--log_rotate_count":
                    if (i + 1 < parts.length) {
                        try { cfg.logRotateCount = Integer.parseInt(parts[++i]); } catch (NumberFormatException e) {}
                    }
                    break;
                case "--checkpoint":
                    if (i + 1 < parts.length) {
                        cfg.checkpointPath = parts[++i];
                    }
                    break;
                case "--force":
                    cfg.force = true;
                    break;
                case "--tps_hysteresis":
                case "--hysteresis":
                    if (i + 1 < parts.length) {
                        try { cfg.tpsHysteresis = Double.parseDouble(parts[++i]); } catch (NumberFormatException e) {}
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
