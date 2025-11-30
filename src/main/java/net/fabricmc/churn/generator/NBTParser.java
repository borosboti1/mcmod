package net.fabricmc.churn.generator;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * NBT (Named Binary Tag) parser for Minecraft world data.
 * Extracts chunk-related information from NBT-encoded data.
 */
public class NBTParser {
    private static final byte TAG_END = 0;
    private static final byte TAG_BYTE = 1;
    private static final byte TAG_SHORT = 2;
    private static final byte TAG_INT = 3;
    private static final byte TAG_LONG = 4;
    private static final byte TAG_FLOAT = 5;
    private static final byte TAG_DOUBLE = 6;
    private static final byte TAG_BYTE_ARRAY = 7;
    private static final byte TAG_STRING = 8;
    private static final byte TAG_LIST = 9;
    private static final byte TAG_COMPOUND = 10;
    private static final byte TAG_INT_ARRAY = 11;
    private static final byte TAG_LONG_ARRAY = 12;

    private DataInputStream dis;

    public NBTParser(byte[] data) throws IOException {
        this.dis = new DataInputStream(new ByteArrayInputStream(data));
    }

    /**
     * Parse root compound tag and return as a map.
     */
    public Map<String, Object> parseRoot() throws IOException {
        byte tagType = dis.readByte();
        if (tagType != TAG_COMPOUND) {
            throw new IOException("Expected compound root, got tag type: " + tagType);
        }
        String name = readString();
        return readCompound();
    }

    /**
     * Read a compound tag as a map of name -> value pairs.
     */
    private Map<String, Object> readCompound() throws IOException {
        Map<String, Object> result = new HashMap<>();
        while (true) {
            byte tagType = dis.readByte();
            if (tagType == TAG_END) break;
            String name = readString();
            Object value = readTag(tagType);
            result.put(name, value);
        }
        return result;
    }

    /**
     * Read a tag based on its type identifier.
     */
    private Object readTag(byte tagType) throws IOException {
        switch (tagType) {
            case TAG_BYTE: return dis.readByte();
            case TAG_SHORT: return dis.readShort();
            case TAG_INT: return dis.readInt();
            case TAG_LONG: return dis.readLong();
            case TAG_FLOAT: return dis.readFloat();
            case TAG_DOUBLE: return dis.readDouble();
            case TAG_BYTE_ARRAY: return readByteArray();
            case TAG_STRING: return readString();
            case TAG_LIST: return readList();
            case TAG_COMPOUND: return readCompound();
            case TAG_INT_ARRAY: return readIntArray();
            case TAG_LONG_ARRAY: return readLongArray();
            default: throw new IOException("Unknown tag type: " + tagType);
        }
    }

    private String readString() throws IOException {
        short length = dis.readShort();
        byte[] data = new byte[length];
        dis.readFully(data);
        return new String(data, "UTF-8");
    }

    private byte[] readByteArray() throws IOException {
        int length = dis.readInt();
        byte[] data = new byte[length];
        dis.readFully(data);
        return data;
    }

    private int[] readIntArray() throws IOException {
        int length = dis.readInt();
        int[] data = new int[length];
        for (int i = 0; i < length; i++) {
            data[i] = dis.readInt();
        }
        return data;
    }

    private long[] readLongArray() throws IOException {
        int length = dis.readInt();
        long[] data = new long[length];
        for (int i = 0; i < length; i++) {
            data[i] = dis.readLong();
        }
        return data;
    }

    private List<Object> readList() throws IOException {
        byte elementType = dis.readByte();
        int length = dis.readInt();
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            result.add(readTag(elementType));
        }
        return result;
    }

    /**
     * Extract chunk information from parsed NBT root.
     */
    public static ChunkData extractChunkData(Map<String, Object> nbtRoot, int chunkX, int chunkZ) {
        ChunkData cd = new ChunkData();
        cd.chunkX = chunkX;
        cd.chunkZ = chunkZ;
        cd.timestamp = System.currentTimeMillis();
        cd.metadata = new HashMap<>();
        
        try {
            // Extract from the Data compound tag
            Map<String, Object> data = (Map<String, Object>) nbtRoot.get("Data");
            if (data != null) {
                // Try to get Y bounds
                Integer yMin = (Integer) data.get("yMin");
                Integer yMax = (Integer) data.get("yMax");
                if (yMin != null) cd.minY = yMin;
                else cd.minY = -64; // Default for 1.18+
                if (yMax != null) cd.maxY = yMax;
                else cd.maxY = 320; // Default for 1.18+
            }

            // Extract sections (store some metadata)
            List<Object> sections = (List<Object>) nbtRoot.get("Sections");
            if (sections != null) {
                cd.metadata.put("section_count", String.valueOf(sections.size()));
                long totalBlocks = 0;
                for (Object sec : sections) {
                    if (sec instanceof Map) {
                        Map<String, Object> section = (Map<String, Object>) sec;
                        // Count blocks in palette if available
                        List<Object> palette = (List<Object>) section.get("Palette");
                        if (palette != null) {
                            totalBlocks += palette.size();
                        }
                    }
                }
                cd.blockCount = (int) Math.min(totalBlocks, Integer.MAX_VALUE);
            } else {
                cd.blockCount = (16 * 16 * (cd.maxY - cd.minY)) / 2;
            }

            // Extract entities
            List<Object> entities = (List<Object>) nbtRoot.get("Entities");
            if (entities != null) {
                for (Object ent : entities) {
                    if (ent instanceof Map) {
                        Map<String, Object> entity = (Map<String, Object>) ent;
                        ChunkData.EntityData ed = new ChunkData.EntityData();
                        
                        Object type = entity.get("id");
                        if (type != null) ed.type = type.toString();
                        
                        List<Object> pos = (List<Object>) entity.get("Pos");
                        if (pos != null && pos.size() >= 3) {
                            ed.x = ((Number) pos.get(0)).doubleValue();
                            ed.y = ((Number) pos.get(1)).doubleValue();
                            ed.z = ((Number) pos.get(2)).doubleValue();
                        }
                        
                        ed.nbt = entity; // Store the entire entity NBT as map
                        cd.entities.add(ed);
                    }
                }
            }

            // Add coordinate metadata
            cd.metadata.put("region_x", String.valueOf(chunkX / 32));
            cd.metadata.put("region_z", String.valueOf(chunkZ / 32));
            cd.metadata.put("local_x", String.valueOf(Math.floorMod(chunkX, 32)));
            cd.metadata.put("local_z", String.valueOf(Math.floorMod(chunkZ, 32)));

        } catch (Exception e) {
            // If extraction fails, log but continue with partial data
            cd.metadata.put("extraction_error", e.getMessage());
        }

        return cd;
    }
}
