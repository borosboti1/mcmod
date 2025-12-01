package net.fabricmc.churn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import net.fabricmc.churn.generator.*;
import net.fabricmc.churn.ui.CommandResponse;
import net.fabricmc.churn.ui.ConsoleLogger;

public class ChurnCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("churn")
                // /churn (shows help)
                .executes(ChurnCommand::executeHelp)
                
                // /churn help
                .then(literal("help")
                    .executes(ChurnCommand::executeHelp))
                
                // /churn settings - show current settings
                .then(literal("settings")
                    .executes(ChurnCommand::executeSettings))
                
                // /churn reset - reset settings to defaults
                .then(literal("reset")
                    .executes(ChurnCommand::executeReset))
                
                // /churn world <name>
                .then(literal("world")
                    .then(argument("name", StringArgumentType.word())
                        .suggests(ChurnCommand::suggestWorlds)
                        .executes(ChurnCommand::executeSetWorld)))
                
                // /churn radius <value>
                .then(literal("radius")
                    .then(argument("value", IntegerArgumentType.integer(1, 2000))
                        .suggests(ChurnCommand::suggestRadius)
                        .executes(ChurnCommand::executeSetRadius)))
                
                // /churn threads <value>
                .then(literal("threads")
                    .then(argument("value", IntegerArgumentType.integer(1, 32))
                        .executes(ChurnCommand::executeSetThreads)))
                
                // /churn output <path>
                .then(literal("output")
                    .then(argument("path", StringArgumentType.greedyString())
                        .executes(ChurnCommand::executeSetOutput)))
                
                // /churn minTps <value>
                .then(literal("minTps")
                    .then(argument("value", StringArgumentType.word())
                        .executes(ChurnCommand::executeSetMinTps)))
                
                // /churn format <type>
                .then(literal("format")
                    .then(argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestOptions(builder, new String[]{"json", "csv"}))
                        .executes(ChurnCommand::executeSetFormat)))
                
                // /churn option <key> <value>
                .then(literal("option")
                    .then(argument("key", StringArgumentType.word())
                        .then(argument("value", StringArgumentType.greedyString())
                            .executes(ChurnCommand::executeSetOption))))
                
                // /churn start [world] [radius] [options]
                .then(literal("start")
                    .then(argument("world", StringArgumentType.word())
                        .suggests(ChurnCommand::suggestWorlds)
                        .then(argument("radius", IntegerArgumentType.integer(1, 2000))
                            .then(argument("options", StringArgumentType.greedyString())
                                .executes(ChurnCommand::executeStart))
                            .executes(ctx -> executeStart(ctx, "")))
                        .executes(ctx -> executeStart(ctx, "", -1)))
                    .executes(ChurnCommand::executeStartWithSettings))
                
                // /churn status [json]
                .then(literal("status")
                    .then(argument("format", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestOptions(builder, new String[]{"json", "plain"}))
                        .executes(ChurnCommand::executeStatus))
                    .executes(ctx -> executeStatus(ctx, "plain")))
                
                // /churn pause
                .then(literal("pause")
                    .executes(ChurnCommand::executePause))
                
                // /churn resume [path]
                .then(literal("resume")
                    .then(argument("path", StringArgumentType.string())
                        .executes(ChurnCommand::executeResume))
                    .executes(ctx -> executeResume(ctx, "")))
                
                // /churn cancel
                .then(literal("cancel")
                    .executes(ChurnCommand::executeCancel))
                
                // /churn postprocess <path>
                .then(literal("postprocess")
                    .then(argument("path", StringArgumentType.string())
                        .executes(ChurnCommand::executePostprocess)))
                
                // /churn clean-checkpoints [path]
                .then(literal("clean-checkpoints")
                    .then(argument("path", StringArgumentType.string())
                        .executes(ChurnCommand::executeCleanCheckpoints))
                    .executes(ctx -> executeCleanCheckpoints(ctx, "churn_checkpoints")))
        );
    }

    // ==================== Help Commands ====================

    private static int executeHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendMessage(Text.literal("§6=== Churn Minecraft Chunk Extraction Mod ==="));
        src.sendMessage(Text.literal(""));
        src.sendMessage(Text.literal("§eBasic Usage:"));
        src.sendMessage(Text.literal("  §7/churn help §r - Show this help message"));
        src.sendMessage(Text.literal("  §7/churn settings §r - Show current settings"));
        src.sendMessage(Text.literal("  §7/churn start §r - Start extraction with current settings"));
        src.sendMessage(Text.literal("  §7/churn status §r - Show extraction progress"));
        src.sendMessage(Text.literal(""));
        src.sendMessage(Text.literal("§eConfiguration Commands:"));
        src.sendMessage(Text.literal("  §7/churn world <name> §r - Set target world (overworld, nether, end)"));
        src.sendMessage(Text.literal("  §7/churn radius <value> §r - Set extraction radius (1-2000 chunks)"));
        src.sendMessage(Text.literal("  §7/churn threads <value> §r - Set worker threads (1-32)"));
        src.sendMessage(Text.literal("  §7/churn output <path> §r - Set output directory"));
        src.sendMessage(Text.literal("  §7/churn minTps <value> §r - Set minimum TPS threshold (0-20)"));
        src.sendMessage(Text.literal("  §7/churn format <json|csv> §r - Set output format"));
        src.sendMessage(Text.literal("  §7/churn reset §r - Reset all settings to defaults"));
        src.sendMessage(Text.literal(""));
        src.sendMessage(Text.literal("§eJob Management:"));
        src.sendMessage(Text.literal("  §7/churn pause §r - Pause current extraction"));
        src.sendMessage(Text.literal("  §7/churn resume §r - Resume from checkpoint"));
        src.sendMessage(Text.literal("  §7/churn cancel §r - Cancel current extraction"));
        src.sendMessage(Text.literal(""));
        src.sendMessage(Text.literal("§eBackward Compatibility:"));
        src.sendMessage(Text.literal("  §7/churn start <world> <radius> [options] §r - Direct start"));
        src.sendMessage(Text.literal(""));
        src.sendMessage(Text.literal("§6Type /churn settings to see current configuration"));
        return 1;
    }

    private static int executeSettings(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String playerId = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : "console";
        ChurnSettings settings = ChurnSettings.getSettings(playerId);

        src.sendMessage(Text.literal("§6=== Current Churn Settings ==="));
        src.sendMessage(Text.literal("§7World: §f" + settings.getWorldId()));
        src.sendMessage(Text.literal("§7Radius: §f" + settings.getRadius() + " chunks"));
        src.sendMessage(Text.literal("§7Threads: §f" + settings.getThreads()));
        src.sendMessage(Text.literal("§7Output Path: §f" + settings.getOutputPath()));
        src.sendMessage(Text.literal("§7Checkpoint Path: §f" + settings.getCheckpointPath()));
        src.sendMessage(Text.literal("§7Minimum TPS: §f" + String.format("%.1f", settings.getMinTps())));
        src.sendMessage(Text.literal("§7Output Format: §f" + settings.getOutputFormat().toUpperCase()));
        src.sendMessage(Text.literal("§7Fast Mode: §f" + (settings.isFastMode() ? "ON" : "OFF")));
        src.sendMessage(Text.literal(""));
        src.sendMessage(Text.literal("§7Use /churn <command> <value> to change settings"));
        return 1;
    }

    private static int executeReset(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String playerId = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : "console";
        ChurnSettings settings = ChurnSettings.getSettings(playerId);
        settings.reset();
        src.sendMessage(Text.literal("§aAll settings reset to defaults"));
        return 1;
    }

    // ==================== Setting Commands ====================

    private static int executeSetWorld(CommandContext<ServerCommandSource> ctx) {
        String world = StringArgumentType.getString(ctx, "name");
        ServerCommandSource src = ctx.getSource();
        String playerId = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : "console";
        ChurnSettings settings = ChurnSettings.getSettings(playerId);

        settings.setWorldId(world);
        src.sendMessage(Text.literal("§aWorld set to: §f" + world));
        return 1;
    }

    private static int executeSetRadius(CommandContext<ServerCommandSource> ctx) {
        int radius = IntegerArgumentType.getInteger(ctx, "value");
        ServerCommandSource src = ctx.getSource();
        String playerId = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : "console";
        ChurnSettings settings = ChurnSettings.getSettings(playerId);

        settings.setRadius(radius);
        src.sendMessage(Text.literal("§aExtraction radius set to: §f" + radius + " chunks"));
        return 1;
    }

    private static int executeSetThreads(CommandContext<ServerCommandSource> ctx) {
        int threads = IntegerArgumentType.getInteger(ctx, "value");
        ServerCommandSource src = ctx.getSource();
        String playerId = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : "console";
        ChurnSettings settings = ChurnSettings.getSettings(playerId);

        settings.setThreads(threads);
        src.sendMessage(Text.literal("§aWorker threads set to: §f" + threads));
        return 1;
    }

    private static int executeSetOutput(CommandContext<ServerCommandSource> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        ServerCommandSource src = ctx.getSource();
        String playerId = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : "console";
        ChurnSettings settings = ChurnSettings.getSettings(playerId);

        settings.setOutputPath(path);
        src.sendMessage(Text.literal("§aOutput path set to: §f" + path));
        return 1;
    }

    private static int executeSetMinTps(CommandContext<ServerCommandSource> ctx) {
        String value = StringArgumentType.getString(ctx, "value");
        ServerCommandSource src = ctx.getSource();
        String playerId = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : "console";
        ChurnSettings settings = ChurnSettings.getSettings(playerId);

        try {
            double tps = Double.parseDouble(value);
            settings.setMinTps(tps);
            src.sendMessage(Text.literal("§aMinimum TPS set to: §f" + String.format("%.1f", tps)));
        } catch (NumberFormatException e) {
            src.sendMessage(Text.literal("§cInvalid TPS value: " + value + " (must be 0.0-20.0)"));
        }
        return 1;
    }

    private static int executeSetFormat(CommandContext<ServerCommandSource> ctx) {
        String format = StringArgumentType.getString(ctx, "type");
        ServerCommandSource src = ctx.getSource();
        String playerId = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : "console";
        ChurnSettings settings = ChurnSettings.getSettings(playerId);

        if (!format.equals("json") && !format.equals("csv")) {
            src.sendMessage(Text.literal("§cInvalid format: " + format + " (must be 'json' or 'csv')"));
            return 0;
        }
        settings.setOutputFormat(format);
        src.sendMessage(Text.literal("§aOutput format set to: §f" + format.toUpperCase()));
        return 1;
    }

    private static int executeSetOption(CommandContext<ServerCommandSource> ctx) {
        String key = StringArgumentType.getString(ctx, "key");
        String value = StringArgumentType.getString(ctx, "value");
        ServerCommandSource src = ctx.getSource();
        String playerId = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : "console";
        ChurnSettings settings = ChurnSettings.getSettings(playerId);

        try {
            switch (key.toLowerCase()) {
                case "world": settings.setWorldId(value); break;
                case "radius": settings.setRadius(Integer.parseInt(value)); break;
                case "threads": settings.setThreads(Integer.parseInt(value)); break;
                case "output": settings.setOutputPath(value); break;
                case "minTps": settings.setMinTps(Double.parseDouble(value)); break;
                case "format": settings.setOutputFormat(value); break;
                case "verbose": settings.setVerbose(value.equalsIgnoreCase("true")); break;
                case "fastMode": settings.setFastMode(value.equalsIgnoreCase("true")); break;
                default:
                    src.sendMessage(Text.literal("§cUnknown option: " + key));
                    return 0;
            }
            src.sendMessage(Text.literal("§aSet §f" + key + "§a to: §f" + value));
        } catch (Exception e) {
            src.sendMessage(Text.literal("§cError setting option: " + e.getMessage()));
            return 0;
        }
        return 1;
    }

    // ==================== Execution Commands ====================

    private static int executeStartWithSettings(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String playerId = src.getPlayer() != null ? src.getPlayer().getUuidAsString() : "console";
        ChurnSettings settings = ChurnSettings.getSettings(playerId);

        JobConfig cfg = settings.toJobConfig();
        
        // Set player context for progress display
        if (src.getPlayer() != null) {
            GeneratorManager.getInstance().setJobPlayer(src.getPlayer(), playerId);
        }
        
        try {
            GeneratorManager.getInstance().startJob(cfg);
            
            // Send professional response
            int totalChunks = (int) ((2L * ((int)Math.ceil(cfg.radius / 16.0) + 1)) * 
                                      (2L * ((int)Math.ceil(cfg.radius / 16.0) + 1)) - 1);
            src.sendMessage(CommandResponse.extractionStarted(cfg.worldId, cfg.radius, totalChunks));
        } catch (Exception e) {
            src.sendMessage(CommandResponse.error("Extraction Failed", e.getMessage(), 
                "Check settings with /churn settings"));
            ConsoleLogger.error("Failed to start extraction: %s", e);
        }
        return 1;
    }

    private static int executeStart(CommandContext<ServerCommandSource> ctx) {
        String world = StringArgumentType.getString(ctx, "world");
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        String options = StringArgumentType.getString(ctx, "options");
        return executeStart(ctx, world, radius, options);
    }

    private static int executeStart(CommandContext<ServerCommandSource> ctx, String options) {
        String world = StringArgumentType.getString(ctx, "world");
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        return executeStart(ctx, world, radius, options);
    }

    private static int executeStart(CommandContext<ServerCommandSource> ctx, String world, int radius) {
        return executeStart(ctx, world, radius, "");
    }

    private static int executeStart(CommandContext<ServerCommandSource> ctx, String world, int radius, String options) {
        ServerCommandSource src = ctx.getSource();

        try {
            Properties p = parseOptions(options);
            JobConfig cfg = JobConfig.fromProperties(p);
            cfg.worldId = world == null || world.isEmpty() ? cfg.worldId : world;
            cfg.radius = radius > 0 ? radius : cfg.radius;
            if (cfg.outputPath == null) cfg.outputPath = "churn_output";
            if (cfg.checkpointPath == null) cfg.checkpointPath = "churn_checkpoints";

            // Detect server world
            java.nio.file.Path worldDir = null;
            try {
                net.minecraft.server.MinecraftServer server = src.getServer();
                if (server != null) {
                    java.nio.file.Path serverDir = server.getRunDirectory();
                    java.nio.file.Path potentialWorld = serverDir.resolve("world");
                    if (java.nio.file.Files.exists(potentialWorld)) {
                        worldDir = potentialWorld;
                        src.sendMessage(Text.literal("§6[Churn] §7Detected world directory: " + worldDir));
                    }
                }
            } catch (Exception e) {
                src.sendMessage(Text.literal("§6[Churn] §7Warning: Could not detect world directory: " + e.getMessage()));
            }

            if (worldDir != null) {
                System.setProperty("churn.worldDir", worldDir.toString());
            }

            GeneratorManager.getInstance().startJob(cfg);
            src.sendMessage(Text.literal("§6[Churn] §aExtraction started: " + cfg.toString()));
        } catch (Exception e) {
            src.sendMessage(Text.literal("§6[Churn] §cFailed to start extraction: " + e.getMessage()));
        }
        return 1;
    }

    private static int executeStatus(CommandContext<ServerCommandSource> ctx) {
        String format = StringArgumentType.getString(ctx, "format");
        return executeStatus(ctx, format);
    }

    private static int executeStatus(CommandContext<ServerCommandSource> ctx, String format) {
        ServerCommandSource src = ctx.getSource();
        try {
            GeneratorManager manager = GeneratorManager.getInstance();
            long total = manager.getChunksTotal();
            long completed = manager.getChunksCompleted();
            
            if (total == 0) {
                src.sendMessage(CommandResponse.errorNoJobRunning());
                return 1;
            }
            
            int percent = (int) ((completed * 100) / total);
            long elapsed = System.currentTimeMillis() - manager.getStartTime();
            double speed = manager.getChunksPerSecond();
            long remaining = speed > 0 ? (long)((total - completed) / speed * 1000) : 0;
            
            src.sendMessage(CommandResponse.status(percent, (int)completed, (int)total, 
                elapsed, speed, remaining));
        } catch (Exception e) {
            src.sendMessage(CommandResponse.error("Status Error", e.getMessage(), 
                "Try /churn start to begin a new job"));
        }
        return 1;
    }

    private static int executePause(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            GeneratorManager.getInstance().pauseCurrentJob();
            src.sendMessage(CommandResponse.extractionPaused("churn_last_job.meta"));
        } catch (Exception e) {
            src.sendMessage(CommandResponse.error("Pause Failed", e.getMessage(), 
                "Try /churn cancel to stop"));
            ConsoleLogger.error("Failed to pause extraction: %s", e);
        }
        return 1;
    }

    private static int executeResume(CommandContext<ServerCommandSource> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        return executeResume(ctx, path);
    }

    private static int executeResume(CommandContext<ServerCommandSource> ctx, String path) {
        ServerCommandSource src = ctx.getSource();
        if (path == null || path.isEmpty()) {
            path = "churn_last_job.meta";
        }
        try {
            GeneratorManager.getInstance().resumeJob(path);
            src.sendMessage(Text.literal("§6[Churn] §aResuming from: " + path));
        } catch (Exception e) {
            src.sendMessage(Text.literal("§6[Churn] §cError resuming: " + e.getMessage()));
        }
        return 1;
    }

    private static int executeCancel(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            long completed = GeneratorManager.getInstance().getChunksCompleted();
            GeneratorManager.getInstance().cancelCurrentJob();
            src.sendMessage(CommandResponse.extractionCancelled((int)completed, "churn_output/partial"));
        } catch (Exception e) {
            src.sendMessage(CommandResponse.error("Cancel Failed", e.getMessage(), 
                "No job may be running"));
            ConsoleLogger.error("Failed to cancel extraction: %s", e);
        }
        return 1;
    }

    private static int executePostprocess(CommandContext<ServerCommandSource> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        ServerCommandSource src = ctx.getSource();
        try {
            GeneratorManager.getInstance().startPostProcess(path);
            src.sendMessage(Text.literal("§6[Churn] §aPostprocessing started: " + path));
        } catch (Exception e) {
            src.sendMessage(Text.literal("§6[Churn] §cError during postprocessing: " + e.getMessage()));
        }
        return 1;
    }

    private static int executeCleanCheckpoints(CommandContext<ServerCommandSource> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        return executeCleanCheckpoints(ctx, path);
    }

    private static int executeCleanCheckpoints(CommandContext<ServerCommandSource> ctx, String path) {
        ServerCommandSource src = ctx.getSource();
        try {
            GeneratorManager.getInstance().cleanCheckpoints(path);
            src.sendMessage(Text.literal("§6[Churn] §aCheckpoints cleaned: " + path));
        } catch (Exception e) {
            src.sendMessage(Text.literal("§6[Churn] §cError cleaning checkpoints: " + e.getMessage()));
        }
        return 1;
    }

    // ==================== Auto-completion Suggestions ====================

    private static CompletableFuture<Suggestions> suggestWorlds(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        String[] worlds = {"overworld", "nether", "end", "custom_world"};
        return suggestOptions(builder, worlds);
    }

    private static CompletableFuture<Suggestions> suggestRadius(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        int[] radii = {16, 32, 64, 128, 256, 512};
        for (int r : radii) {
            builder.suggest(r);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOptions(SuggestionsBuilder builder, String[] options) {
        String input = builder.getRemaining().toLowerCase();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input)) {
                builder.suggest(option);
            }
        }
        return builder.buildFuture();
    }

    // ==================== Option Parsing (Legacy) ====================

    private static Properties parseOptions(String options) throws Exception {
        Properties p = new Properties();
        if (options == null || options.isEmpty()) return p;
        String s = options.trim();

        // JSON file reference
        if (s.startsWith("@")) {
            String path = s.substring(1).trim();
            java.nio.file.Path pth = Paths.get(path);
            if (Files.exists(pth)) {
                if (path.endsWith(".properties")) {
                    try (java.io.InputStream is = Files.newInputStream(pth)) { p.load(is); }
                } else if (path.endsWith(".json")) {
                    String json = new String(Files.readAllBytes(pth));
                    parseJsonToProperties(json, p);
                }
            }
            return p;
        }

        // Inline JSON
        if (s.startsWith("{") && s.endsWith("}")) {
            parseJsonToProperties(s, p);
            return p;
        }

        return p;
    }

    private static void parseJsonToProperties(String json, Properties p) {
        try {
            Object root = parseJson(json);
            if (root instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> m = (java.util.Map<String, Object>) root;
                flattenMap("", m, p);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    private static void flattenMap(String prefix, java.util.Map<String, Object> map, Properties p) {
        for (java.util.Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            Object v = e.getValue();
            if (v instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> sub = (java.util.Map<String, Object>) v;
                flattenMap(key, sub, p);
            } else if (v == null) {
                p.setProperty(key, "");
            } else {
                p.setProperty(key, v.toString());
            }
        }
    }

    private static Object parseJson(String json) throws Exception {
        java.io.StringReader r = new java.io.StringReader(json);
        JsonTok tok = new JsonTok(r);
        return tok.parseValue();
    }

    private static final class JsonTok {
        private final java.io.PushbackReader in;
        public JsonTok(java.io.Reader r) { this.in = new java.io.PushbackReader(r); }

        public Object parseValue() throws Exception {
            skipWs();
            int c = in.read();
            if (c == -1) return null;
            in.unread(c);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            String tok = parseLiteralToken();
            if (tok == null) return null;
            if (tok.equals("true")) return Boolean.TRUE;
            if (tok.equals("false")) return Boolean.FALSE;
            if (tok.equals("null")) return null;
            if (tok.matches("-?\\d+(\\\\.\\d+)?([eE][+-]?\\d+)?")) {
                if (tok.contains(".") || tok.contains("e") || tok.contains("E")) return Double.parseDouble(tok);
                try { return Long.parseLong(tok); } catch (NumberFormatException ex) { return Double.parseDouble(tok); }
            }
            return tok;
        }

        private java.util.Map<String,Object> parseObject() throws Exception {
            java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
            expectChar('{');
            skipWs();
            int c = in.read();
            if (c == '}') return m;
            in.unread(c);
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                expectChar(':');
                Object v = parseValue();
                m.put(key, v);
                skipWs();
                c = in.read();
                if (c == ',') continue;
                if (c == '}') break;
                throw new IllegalStateException("Unexpected char: " + (char)c);
            }
            return m;
        }

        private java.util.List<Object> parseArray() throws Exception {
            java.util.List<Object> a = new java.util.ArrayList<>();
            expectChar('[');
            skipWs();
            int c = in.read();
            if (c == ']') return a;
            in.unread(c);
            while (true) {
                Object v = parseValue();
                a.add(v);
                skipWs();
                c = in.read();
                if (c == ',') continue;
                if (c == ']') break;
                throw new IllegalStateException("Unexpected char: " + (char)c);
            }
            return a;
        }

        private String parseString() throws Exception {
            expectChar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c = in.read();
                if (c == -1) throw new IllegalStateException("Unterminated string");
                if (c == '"') break;
                if (c == '\\') {
                    int d = in.read();
                    if (d == -1) throw new IllegalStateException("Unterminated escape");
                    switch (d) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            char[] hex = new char[4];
                            for (int i=0;i<4;i++) { int h = in.read(); if (h==-1) throw new IllegalStateException("Bad unicode"); hex[i]=(char)h; }
                            sb.append((char)Integer.parseInt(new String(hex),16));
                            break;
                        default: sb.append((char)d);
                    }
                } else {
                    sb.append((char)c);
                }
            }
            return sb.toString();
        }

        private String parseLiteralToken() throws Exception {
            StringBuilder sb = new StringBuilder();
            skipWs();
            while (true) {
                int c = in.read();
                if (c == -1) break;
                char ch = (char)c;
                if (Character.isWhitespace(ch) || ch==',' || ch==']' || ch=='}' || ch==':') { in.unread(c); break; }
                sb.append(ch);
            }
            return sb.length() == 0 ? null : sb.toString();
        }

        private void skipWs() throws Exception { int c; while ((c = in.read()) != -1) { if (!Character.isWhitespace((char)c)) { in.unread(c); break; } } }
        private void expectChar(char ch) throws Exception { int c = in.read(); if (c != ch) throw new IllegalStateException("Expected '" + ch + "' but got '" + (char)c + "'"); }
    }
}
