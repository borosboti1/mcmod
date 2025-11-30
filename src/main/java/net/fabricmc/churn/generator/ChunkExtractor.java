package net.fabricmc.churn.generator;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Chunk extraction engine for Minecraft Anvil format (.mca region files).
 * Parses NBT data, extracts chunks by radius, and produces structured output.
 */
public class ChunkExtractor {
    private static final int REGION_SIZE = 32; // 32x32 chunks per region
    private static final int CHUNK_SIZE = 16;  // 16x16 blocks per chunk
    private final WorldNavigator navigator;
    private final ProgressLogger logger;

    public ChunkExtractor(WorldNavigator navigator, ProgressLogger logger) {
        this.navigator = navigator;
        this.logger = logger;
    }

    /**
     * Extract all chunks within radius from center, returning chunk data.
     */
    public List<ChunkData> extractChunksInRadius(int centerChunkX, int centerChunkZ, int radiusChunks) throws Exception {
        List<ChunkData> chunks = new ArrayList<>();
        
        int minChunkX = centerChunkX - radiusChunks;
        int maxChunkX = centerChunkX + radiusChunks;
        int minChunkZ = centerChunkZ - radiusChunks;
        int maxChunkZ = centerChunkZ + radiusChunks;

        // Group chunks by region file for efficient I/O
        Map<String, List<int[]>> chunksByRegion = new TreeMap<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                int regionX = Math.floorDiv(cx, REGION_SIZE);
                int regionZ = Math.floorDiv(cz, REGION_SIZE);
                String regionKey = regionX + "." + regionZ;
                chunksByRegion.computeIfAbsent(regionKey, k -> new ArrayList<>()).add(new int[]{cx, cz});
            }
        }

        // Process each region file
        for (Map.Entry<String, List<int[]>> entry : chunksByRegion.entrySet()) {
            String regionKey = entry.getKey();
            List<int[]> chunkCoords = entry.getValue();
            int[] parts = regionKey.split("\\.") .length == 2 ? 
                new int[]{Integer.parseInt(regionKey.split("\\.")[0]), Integer.parseInt(regionKey.split("\\.")[1])} : new int[2];
            
            try {
                Path regionFile = navigator.getRegionFile(parts[0], parts[1]);
                if (regionFile != null && Files.exists(regionFile)) {
                    for (int[] coord : chunkCoords) {
                        ChunkData cd = extractChunkFromRegion(regionFile, coord[0], coord[1]);
                        if (cd != null) chunks.add(cd);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Churn] failed to extract region " + regionKey + ": " + e.getMessage());
            }
        }

        return chunks;
    }

    /**
     * Extract a single chunk from a region file using NBT parsing.
     */
    private ChunkData extractChunkFromRegion(Path regionFile, int chunkX, int chunkZ) throws IOException {
        int localX = Math.floorMod(chunkX, REGION_SIZE);
        int localZ = Math.floorMod(chunkZ, REGION_SIZE);
        int offset = (localX + localZ * REGION_SIZE) * 4096;

        byte[] header = new byte[4];
        try (RandomAccessFile raf = new RandomAccessFile(regionFile.toFile(), "r")) {
            raf.seek((long) offset);
            raf.readFully(header);
            
            int dataLength = ((header[0] & 0xFF) << 24) | ((header[1] & 0xFF) << 16) | ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
            if (dataLength == 0) return null; // empty chunk
            
            byte compression = (byte) raf.read();
            byte[] compressedData = new byte[dataLength - 1];
            raf.readFully(compressedData);

            byte[] decompressed = decompress(compression, compressedData);
            return parseNBT(chunkX, chunkZ, decompressed);
        }
    }

    /**
     * Decompress chunk data (gzip or zlib).
     */
    private byte[] decompress(byte compression, byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        InputStream inflater;
        if (compression == 1) {
            // gzip
            inflater = new GZIPInputStream(bais);
        } else if (compression == 2) {
            // zlib/deflate
            inflater = new InflaterInputStream(bais);
        } else {
            throw new IOException("Unknown compression: " + compression);
        }

        byte[] buffer = new byte[8192];
        int count;
        while ((count = inflater.read(buffer)) != -1) {
            baos.write(buffer, 0, count);
        }
        inflater.close();
        return baos.toByteArray();
    }

    /**
     * Parse NBT (Named Binary Tag) data and extract chunk information.
     * Uses full NBT parsing to extract chunk metadata, entities, and block data.
     */
    private ChunkData parseNBT(int chunkX, int chunkZ, byte[] data) throws IOException {
        try {
            NBTParser parser = new NBTParser(data);
            Map<String, Object> nbtRoot = parser.parseRoot();
            return NBTParser.extractChunkData(nbtRoot, chunkX, chunkZ);
        } catch (Exception e) {
            // If NBT parsing fails, return a stub chunk with error metadata
            System.err.println("[Churn] NBT parsing failed for chunk " + chunkX + "," + chunkZ + ": " + e.getMessage());
            ChunkData cd = new ChunkData();
            cd.chunkX = chunkX;
            cd.chunkZ = chunkZ;
            cd.minY = -64;
            cd.maxY = 320;
            cd.blockCount = 0;
            cd.timestamp = System.currentTimeMillis();
            cd.metadata = new java.util.HashMap<>();
            cd.metadata.put("error", e.getMessage());
            cd.metadata.put("region_x", String.valueOf(chunkX / REGION_SIZE));
            cd.metadata.put("region_z", String.valueOf(chunkZ / REGION_SIZE));
            return cd;
        }
    }
}
