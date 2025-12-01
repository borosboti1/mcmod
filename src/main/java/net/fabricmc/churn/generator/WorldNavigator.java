package net.fabricmc.churn.generator;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.churn.ui.ConsoleLogger;

/**
 * Navigates Minecraft world structure: locates world directories,
 * finds region files, validates world integrity.
 * 
 * Supports dimension name mapping:
 * - overworld/world → world/
 * - nether/the_nether → world/DIM-1/
 * - end/the_end → world/DIM1/
 */
public class WorldNavigator {
    private final Path worldRoot;
    private final Path regionDir;
    
    // Map user-friendly dimension names to actual directory paths
    private static final Map<String, String> DIMENSION_MAP = new HashMap<>();
    static {
        DIMENSION_MAP.put("overworld", "world");
        DIMENSION_MAP.put("world", "world");
        DIMENSION_MAP.put("default", "world");
        DIMENSION_MAP.put("nether", "world/DIM-1");
        DIMENSION_MAP.put("the_nether", "world/DIM-1");
        DIMENSION_MAP.put("end", "world/DIM1");
        DIMENSION_MAP.put("the_end", "world/DIM1");
    }

    public WorldNavigator(String worldId, Path baseDir) throws Exception {
        // Clean up dimension ID (remove minecraft: prefix if present)
        String cleanId = worldId.contains(":") ? worldId.split(":")[1] : worldId;
        
        // Map dimension name to actual directory path
        String mappedPath = DIMENSION_MAP.getOrDefault(cleanId.toLowerCase(), cleanId);
        
        // Try to find the world directory
        this.worldRoot = findWorldDirectory(baseDir, mappedPath, cleanId);
        if (worldRoot == null) {
            // Provide helpful error message
            List<String> available = getAvailableDimensions(baseDir);
            String searchedPaths = String.join(", ", getSearchPaths(baseDir, mappedPath));
            throw new Exception(
                "Dimension '" + cleanId + "' not found.\n" +
                "Searched: " + searchedPaths + "\n" +
                "Available dimensions: " + String.join(", ", available)
            );
        }

        ConsoleLogger.init("[NAV] World located: " + worldRoot.toAbsolutePath().toString());

        // Region directory (depends on dimension)
        String dimPath = cleanId.toLowerCase().contains("nether") ? "DIM-1/region" : 
                         cleanId.toLowerCase().contains("end") ? "DIM1/region" : "region";
        this.regionDir = worldRoot.resolve(dimPath);
        
        if (!Files.exists(regionDir)) {
            throw new Exception("Region directory not found: " + regionDir);
        }
    }

    /**
     * Map user-friendly dimension names to actual directory paths
     */
    public static String mapDimensionName(String userFriendlyName) {
        return DIMENSION_MAP.getOrDefault(userFriendlyName.toLowerCase(), userFriendlyName);
    }

    /**
     * Get list of possible search paths for debugging
     */
    private static List<String> getSearchPaths(Path baseDir, String mappedPath) {
        List<String> paths = new ArrayList<>();
        paths.add(baseDir.resolve(mappedPath).toString());
        paths.add(baseDir.resolve("saves").resolve(mappedPath).toString());
        paths.add(baseDir.resolve("worlds").resolve(mappedPath).toString());
        return paths;
    }

    /**
     * Locate world directory by name (search in multiple locations)
     */
    private Path findWorldDirectory(Path baseDir, String mappedPath, String originalName) throws Exception {
        // Try all possible locations
        Path[] candidates = {
            baseDir.resolve(mappedPath),
            baseDir.resolve("saves").resolve(mappedPath),
            baseDir.resolve("worlds").resolve(mappedPath),
            // Fallback: try original name in case of custom dimensions
            baseDir.resolve(originalName),
            baseDir.resolve("saves").resolve(originalName),
            baseDir.resolve("worlds").resolve(originalName)
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
     * Discover all available dimensions in the world directory
     */
    public static List<String> getAvailableDimensions(Path baseDir) {
        List<String> dimensions = new ArrayList<>();
        
        // Always add overworld if world/ exists
        if (Files.exists(baseDir.resolve("world"))) {
            dimensions.add("overworld");
        }
        
        // Check for Nether
        if (Files.exists(baseDir.resolve("world/DIM-1"))) {
            dimensions.add("nether");
        }
        
        // Check for End
        if (Files.exists(baseDir.resolve("world/DIM1"))) {
            dimensions.add("end");
        }
        
        // Check for custom worlds in saves/
        Path savesDir = baseDir.resolve("saves");
        if (Files.exists(savesDir) && Files.isDirectory(savesDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(savesDir)) {
                for (Path path : stream) {
                    if (Files.isDirectory(path) && Files.exists(path.resolve("level.dat"))) {
                        dimensions.add(path.getFileName().toString());
                    }
                }
            } catch (Exception e) {
                // Silently ignore enumeration errors
            }
        }
        
        // Check for worlds in worlds/
        Path worldsDir = baseDir.resolve("worlds");
        if (Files.exists(worldsDir) && Files.isDirectory(worldsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(worldsDir)) {
                for (Path path : stream) {
                    if (Files.isDirectory(path) && Files.exists(path.resolve("level.dat"))) {
                        dimensions.add(path.getFileName().toString());
                    }
                }
            } catch (Exception e) {
                // Silently ignore enumeration errors
            }
        }
        
        return dimensions.isEmpty() ? Arrays.asList("overworld") : dimensions;
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
