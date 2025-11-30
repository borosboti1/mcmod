package net.fabricmc.churn.generator;

import java.nio.file.*;
import java.util.*;

/**
 * Navigates Minecraft world structure: locates world directories,
 * finds region files, validates world integrity.
 */
public class WorldNavigator {
    private final Path worldRoot;
    private final Path regionDir;

    public WorldNavigator(String worldId, Path baseDir) throws Exception {
        // worldId format: "minecraft:overworld" or just "world_name"
        String cleanId = worldId.contains(":") ? worldId.split(":")[1] : worldId;
        
        // Try common world directory paths
        this.worldRoot = findWorldDirectory(baseDir, cleanId);
        if (worldRoot == null) {
            throw new Exception("World '" + cleanId + "' not found in " + baseDir);
        }

        // Region directory (depends on dimension)
        String dimPath = cleanId.contains("nether") ? "DIM-1/data" : 
                         cleanId.contains("end") ? "DIM1/data" : "region";
        this.regionDir = worldRoot.resolve(dimPath);
        
        if (!Files.exists(regionDir)) {
            throw new Exception("Region directory not found: " + regionDir);
        }
    }

    /**
     * Locate world directory by name (search in saves/ or worlds/).
     */
    private Path findWorldDirectory(Path baseDir, String worldName) throws Exception {
        // Common patterns: <baseDir>/saves/<worldName>, <baseDir>/worlds/<worldName>, <baseDir>/<worldName>
        Path[] candidates = {
            baseDir.resolve("saves").resolve(worldName),
            baseDir.resolve("worlds").resolve(worldName),
            baseDir.resolve(worldName)
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                // Verify it's a valid world (has level.dat)
                if (Files.exists(candidate.resolve("level.dat"))) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Get region file path for given region coordinates.
     */
    public Path getRegionFile(int regionX, int regionZ) {
        return regionDir.resolve("r." + regionX + "." + regionZ + ".mca");
    }

    /**
     * List all region files in the world.
     */
    public List<Path> listRegionFiles() throws Exception {
        List<Path> regions = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(regionDir, "r.*.mca")) {
            for (Path file : stream) {
                regions.add(file);
            }
        }
        return regions;
    }

    /**
     * Get world root directory.
     */
    public Path getWorldRoot() {
        return worldRoot;
    }

    /**
     * Get region directory.
     */
    public Path getRegionDirectory() {
        return regionDir;
    }

    /**
     * Validate world structure and accessibility.
     */
    public List<String> validateWorld() {
        List<String> issues = new ArrayList<>();
        
        if (!Files.exists(worldRoot)) {
            issues.add("World directory does not exist: " + worldRoot);
        } else if (!Files.isDirectory(worldRoot)) {
            issues.add("World path is not a directory: " + worldRoot);
        }

        Path levelDat = worldRoot.resolve("level.dat");
        if (!Files.exists(levelDat)) {
            issues.add("level.dat not found");
        }

        if (!Files.exists(regionDir)) {
            issues.add("Region directory does not exist: " + regionDir);
        } else if (!Files.isDirectory(regionDir)) {
            issues.add("Region path is not a directory: " + regionDir);
        }

        try {
            List<Path> regions = listRegionFiles();
            if (regions.isEmpty()) {
                issues.add("No region files found");
            }
        } catch (Exception e) {
            issues.add("Failed to list regions: " + e.getMessage());
        }

        return issues;
    }
}
