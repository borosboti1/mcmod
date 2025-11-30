package net.fabricmc.churn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.fabricmc.churn.generator.GeneratorManager;
import net.fabricmc.churn.generator.JobConfig;

public class ChurnCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("churn")
                .then(literal("start")
                    .then(argument("world", StringArgumentType.word())
                        .then(argument("radius", IntegerArgumentType.integer(16))
                            .then(argument("options", StringArgumentType.greedyString())
                                .executes(ChurnCommand::executeStart)
                            )
                            .executes(ctx -> executeStart(ctx, "")) // without options
                        )
                    )
                )
                .then(literal("status")
                    .then(argument("options", StringArgumentType.greedyString())
                        .executes(ChurnCommand::executeStatus)
                    )
                    .executes(ctx -> executeStatus(ctx, "")) // without options
                )
                .then(literal("cancel")
                    .executes(ChurnCommand::executeCancel)
                )
                .then(literal("pause")
                    .executes(ChurnCommand::executePause)
                )
                .then(literal("resume")
                    .then(argument("path", StringArgumentType.string())
                        .executes(ChurnCommand::executeResume)
                    )
                )
                .then(literal("clean-checkpoints")
                    .then(argument("path", StringArgumentType.string())
                        .executes(ChurnCommand::executeCleanCheckpoints)
                    )
                )
                .then(literal("postprocess")
                    .then(argument("path", StringArgumentType.string())
                        .executes(ChurnCommand::executePostprocess)
                    )
                )
                .then(literal("help")
                    .executes(ChurnCommand::executeHelp)
                )
        );
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

    private static int executeStart(CommandContext<ServerCommandSource> ctx, String world, int radius, String options) {
        ctx.getSource().sendMessage(Text.literal("§6[Churn] §eStart parancs: világ='" + world + "', sugár=" + radius + ", opciók='" + options + "'"));
        try {
            Properties p = parseOptions(options);
            JobConfig cfg = JobConfig.fromProperties(p);
            cfg.worldId = world == null || world.isEmpty() ? cfg.worldId : world;
            cfg.radius = radius > 0 ? radius : cfg.radius;
            if (cfg.outputPath == null) cfg.outputPath = "churn_output";
            if (cfg.checkpointPath == null) cfg.checkpointPath = "churn_checkpoints";
            GeneratorManager.getInstance().startJob(cfg);
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §aJob elindítva: " + cfg.toString()));
        } catch (Exception e) {
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §cNem sikerült elindítani a jobot: " + e.getMessage()));
        }
        return 1;
    }

    private static int executeStatus(CommandContext<ServerCommandSource> ctx) {
        String options = StringArgumentType.getString(ctx, "options");
        return executeStatus(ctx, options);
    }

    private static int executeStatus(CommandContext<ServerCommandSource> ctx, String options) {
        ctx.getSource().sendMessage(Text.literal("§6[Churn] §eStatus parancs: opciók='" + options + "'"));
        try {
            boolean json = options != null && options.contains("json");
            String s = json ? GeneratorManager.getInstance().getStatusJson() : GeneratorManager.getInstance().getStatus();
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §7" + s));
        } catch (Exception e) {
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §cHiba státusz lekérdezésekor: " + e.getMessage()));
        }
        return 1;
    }

    private static int executeCancel(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendMessage(Text.literal("§6[Churn] §cCancel parancs - folyamat megszakítva"));
        try {
            GeneratorManager.getInstance().cancelCurrentJob();
        } catch (Exception e) {
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §cHiba megszakításkor: " + e.getMessage()));
        }
        return 1;
    }

    private static int executePause(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendMessage(Text.literal("§6[Churn] §ePause parancs - folyamat szüneteltetve"));
        try {
            GeneratorManager.getInstance().pauseCurrentJob();
        } catch (Exception e) {
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §cHiba szüneteltetéskor: " + e.getMessage()));
        }
        return 1;
    }

    private static int executeResume(CommandContext<ServerCommandSource> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        ctx.getSource().sendMessage(Text.literal("§6[Churn] §aResume parancs: útvonal='" + path + "'"));
        try {
            GeneratorManager.getInstance().resumeJob(path);
        } catch (Exception e) {
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §cHiba folytatáskor: " + e.getMessage()));
        }
        return 1;
    }

    private static int executeCleanCheckpoints(CommandContext<ServerCommandSource> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        ctx.getSource().sendMessage(Text.literal("§6[Churn] §bClean-checkpoints parancs: útvonal='" + path + "'"));
        try {
            GeneratorManager.getInstance().cleanCheckpoints(path);
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §aCheckpoint-ok törölve: " + path));
        } catch (Exception e) {
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §cHiba checkpoint törléskor: " + e.getMessage()));
        }
        return 1;
    }

    private static int executePostprocess(CommandContext<ServerCommandSource> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        ctx.getSource().sendMessage(Text.literal("§6[Churn] §dPostprocess parancs: útvonal='" + path + "'"));
        try {
            GeneratorManager.getInstance().startPostProcess(path);
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §aPostprocess elindítva: " + path));
        } catch (Exception e) {
            ctx.getSource().sendMessage(Text.literal("§6[Churn] §cHiba postprocess során: " + e.getMessage()));
        }
        return 1;
    }

    private static Properties parseOptions(String options) throws Exception {
        Properties p = new Properties();
        if (options == null) return p;
        String s = options.trim();
        if (s.isEmpty()) return p;

        // If options is a file reference like @path/to/file.properties
        if (s.startsWith("@")) {
            String path = s.substring(1).trim();
            java.nio.file.Path pth = Paths.get(path);
            if (Files.exists(pth)) {
                if (path.endsWith(".properties")) {
                    try (java.io.InputStream is = Files.newInputStream(pth)) { p.load(is); }
                    return p;
                } else if (path.endsWith(".json")) {
                    String json = new String(Files.readAllBytes(pth));
                    parseJsonToProperties(json, p);
                    return p;
                }
            }
        }

        // If single token is an existing file, auto-detect
        try {
            java.nio.file.Path possible = Paths.get(s);
            if (Files.exists(possible)) {
                String fn = possible.getFileName().toString().toLowerCase();
                if (fn.endsWith(".properties")) {
                    try (java.io.InputStream is = Files.newInputStream(possible)) { p.load(is); }
                    return p;
                } else if (fn.endsWith(".json")) {
                    String json = new String(Files.readAllBytes(possible));
                    parseJsonToProperties(json, p);
                    return p;
                }
            }
        } catch (Exception ignored) {}

        // If inline JSON object provided, parse it
        if (s.startsWith("{") && s.endsWith("}")) {
            parseJsonToProperties(s, p);
            return p;
        }

        // Tokenize respecting quoted strings
        java.util.List<String> tokens = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (Character.isWhitespace(c) && !inQuote) {
                if (cur.length() > 0) { tokens.add(cur.toString()); cur.setLength(0); }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) tokens.add(cur.toString());

        for (int i = 0; i < tokens.size(); i++) {
            String tok = tokens.get(i);
            if (tok.startsWith("--")) tok = tok.substring(2);
            int eq = tok.indexOf('=');
            if (eq >= 0) {
                String k = tok.substring(0, eq);
                String v = tok.substring(eq + 1);
                p.setProperty(k, v);
            } else {
                // if next token exists and does not start with --, treat as value
                if (i + 1 < tokens.size() && !tokens.get(i + 1).startsWith("--") && !tokens.get(i + 1).contains("=")) {
                    p.setProperty(tok, tokens.get(i + 1));
                    i++; // consume
                } else {
                    p.setProperty(tok, "true");
                }
            }
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
            } else if (root instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> l = (java.util.List<Object>) root;
                flattenList("root", l, p);
            } else if (root != null) {
                p.setProperty("value", root.toString());
            }
        } catch (Exception e) {
            // if parsing fails, leave properties empty
        }
    }

    // Flatten nested map into properties using dot notation.
    private static void flattenMap(String prefix, java.util.Map<String, Object> map, Properties p) {
        for (java.util.Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            Object v = e.getValue();
            if (v instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> sub = (java.util.Map<String, Object>) v;
                flattenMap(key, sub, p);
            } else if (v instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> l = (java.util.List<Object>) v;
                // If list is primitive-only, join with comma, otherwise index them
                boolean allPrim = true;
                for (Object it : l) if (it instanceof java.util.Map || it instanceof java.util.List) { allPrim = false; break; }
                if (allPrim) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < l.size(); i++) {
                        if (i > 0) sb.append(',');
                        sb.append(l.get(i) == null ? "" : l.get(i).toString());
                    }
                    p.setProperty(key, sb.toString());
                } else {
                    flattenList(key, l, p);
                }
            } else if (v == null) {
                p.setProperty(key, "");
            } else {
                p.setProperty(key, v.toString());
            }
        }
    }

    private static void flattenList(String prefix, java.util.List<Object> list, Properties p) {
        for (int i = 0; i < list.size(); i++) {
            Object v = list.get(i);
            String key = prefix + "." + i;
            if (v instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> m = (java.util.Map<String, Object>) v;
                flattenMap(key, m, p);
            } else if (v instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> l = (java.util.List<Object>) v;
                flattenList(key, l, p);
            } else if (v == null) {
                p.setProperty(key, "");
            } else {
                p.setProperty(key, v.toString());
            }
        }
    }

    // Minimal JSON tokenizer+parser returning Map/String/Number/Boolean/List
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
            // literal: number, true, false, null
            String tok = parseLiteralToken();
            if (tok == null) return null;
            if (tok.equals("true")) return Boolean.TRUE;
            if (tok.equals("false")) return Boolean.FALSE;
            if (tok.equals("null")) return null;
            // number
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
            if (c == '}') return m; // empty
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
                throw new IllegalStateException("Unexpected char in object: " + (char)c);
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
                throw new IllegalStateException("Unexpected char in array: " + (char)c);
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
                            for (int i=0;i<4;i++) { int h = in.read(); if (h==-1) throw new IllegalStateException("Bad unicode escape"); hex[i]=(char)h; }
                            sb.append((char)Integer.parseInt(new String(hex),16));
                            break;
                        default:
                            sb.append((char)d);
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
            String s = sb.length() == 0 ? null : sb.toString();
            return s;
        }

        private void skipWs() throws Exception { int c; while ((c = in.read()) != -1) { if (!Character.isWhitespace((char)c)) { in.unread(c); break; } } }

        private void expectChar(char ch) throws Exception { int c = in.read(); if (c != ch) throw new IllegalStateException("Expected '" + ch + "' but got '" + (char)c + "'"); }
    }

    private static int executeHelp(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendMessage(Text.literal("§6=== Churn Mod Segítség ==="));
        ctx.getSource().sendMessage(Text.literal("§e/churn start <világ> <sugár> [opciók] §7- Chunk reverse engineering indítása"));
        ctx.getSource().sendMessage(Text.literal("§e/churn status [opciók] §7- Folyamat státusz lekérdezése"));
        ctx.getSource().sendMessage(Text.literal("§e/churn cancel §7- Folyamat megszakítása"));
        ctx.getSource().sendMessage(Text.literal("§e/churn pause §7- Folyamat szüneteltetése"));
        ctx.getSource().sendMessage(Text.literal("§e/churn resume <útvonal> §7- Folyamat folytatása"));
        ctx.getSource().sendMessage(Text.literal("§e/churn clean-checkpoints <útvonal> §7- Checkpoint-ok törlése"));
        ctx.getSource().sendMessage(Text.literal("§e/churn postprocess <útvonal> §7- Adatok posztprocesszálása"));
        return 1;
    }

    // Segédmetódusok a biztonságos argumentum kinyerésére
    private static String safeGetString(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (Exception e) {
            return "";
        }
    }

    private static int safeGetInt(CommandContext<ServerCommandSource> ctx, String name, int defaultValue) {
        try {
            return IntegerArgumentType.getInteger(ctx, name);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}