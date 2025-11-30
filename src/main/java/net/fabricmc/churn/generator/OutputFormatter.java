package net.fabricmc.churn.generator;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Formats extracted chunk data to JSON or CSV output.
 */
public class OutputFormatter {
    private final Path outputDir;

    public OutputFormatter(Path outputDir) throws IOException {
        this.outputDir = outputDir;
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
    }

    /**
     * Write chunks to JSON format (one file per chunk or combined).
     */
    public void writeJSON(List<ChunkData> chunks, boolean combined) throws IOException {
        if (combined) {
            writeCombinedJSON(chunks);
        } else {
            for (ChunkData chunk : chunks) {
                writeChunkJSON(chunk);
            }
        }
    }

    private void writeCombinedJSON(List<ChunkData> chunks) throws IOException {
        Path outFile = outputDir.resolve("chunks.json");
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(outFile))) {
            w.println("[");
            for (int i = 0; i < chunks.size(); i++) {
                if (i > 0) w.println(",");
                w.print(chunkToJSON(chunks.get(i)));
            }
            w.println("\n]");
        }
        System.out.println("[Churn] wrote combined JSON to " + outFile);
    }

    private void writeChunkJSON(ChunkData chunk) throws IOException {
        String filename = "chunk_" + chunk.chunkX + "_" + chunk.chunkZ + ".json";
        Path outFile = outputDir.resolve(filename);
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(outFile))) {
            w.println(chunkToJSON(chunk));
        }
    }

    private String chunkToJSON(ChunkData chunk) {
        StringBuilder sb = new StringBuilder();
        sb.append("  {\n");
        sb.append("    \"x\": ").append(chunk.chunkX).append(",\n");
        sb.append("    \"z\": ").append(chunk.chunkZ).append(",\n");
        sb.append("    \"minY\": ").append(chunk.minY).append(",\n");
        sb.append("    \"maxY\": ").append(chunk.maxY).append(",\n");
        sb.append("    \"blocks\": ").append(chunk.blockCount).append(",\n");
        sb.append("    \"timestamp\": ").append(chunk.timestamp).append(",\n");
        sb.append("    \"entities\": ").append(chunk.entities.size()).append(",\n");
        sb.append("    \"metadata\": {");
        boolean first = true;
        for (Map.Entry<String, String> e : chunk.metadata.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(e.getKey()).append("\": \"").append(e.getValue()).append("\"");
        }
        sb.append("}\n");
        sb.append("  }");
        return sb.toString();
    }

    /**
     * Write chunks to CSV format.
     */
    public void writeCSV(List<ChunkData> chunks) throws IOException {
        Path outFile = outputDir.resolve("chunks.csv");
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(outFile))) {
            // Header
            w.println("chunkX,chunkZ,minY,maxY,blockCount,entityCount,timestamp");
            // Data
            for (ChunkData chunk : chunks) {
                w.println(chunk.chunkX + "," + chunk.chunkZ + "," + chunk.minY + "," + chunk.maxY +
                        "," + chunk.blockCount + "," + chunk.entities.size() + "," + chunk.timestamp);
            }
        }
        System.out.println("[Churn] wrote CSV to " + outFile);
    }
}
