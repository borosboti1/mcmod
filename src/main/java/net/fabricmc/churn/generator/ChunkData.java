package net.fabricmc.churn.generator;

import java.util.*;

/**
 * Structured representation of extracted chunk data.
 * Contains blocks, entities, biomes, and metadata.
 */
public class ChunkData {
    public int chunkX;
    public int chunkZ;
    public int minY;
    public int maxY;
    public int blockCount;
    public long timestamp;
    public Map<String, String> metadata = new HashMap<>();
    public List<EntityData> entities = new ArrayList<>();
    public Map<String, Integer> blockPalette = new HashMap<>();

    @Override
    public String toString() {
        return "ChunkData{" +
                "x=" + chunkX + ", z=" + chunkZ +
                ", y=[" + minY + "," + maxY + "]" +
                ", blocks=" + blockCount +
                ", entities=" + entities.size() +
                ", meta=" + metadata.size() +
                '}';
    }

    /**
     * Simple entity representation.
     */
    public static class EntityData {
        public String type;     // e.g., "minecraft:cow"
        public double x, y, z;
        public Map<String, Object> nbt = new HashMap<>();

        @Override
        public String toString() {
            return type + "@(" + x + "," + y + "," + z + ")";
        }
    }
}
