package net.fabricmc.churn.generator;

/**
 * Placeholder result from worker generation. In a full implementation this would
 * contain block sections, biome data, tile entity NBT, and structure pieces.
 */
public class ChurnBuildResult {
    public final int chunkX;
    public final int chunkZ;
    public final byte[] data; // placeholder serialized data

    public ChurnBuildResult(int chunkX, int chunkZ, byte[] data) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.data = data;
    }

    @Override
    public String toString() {
        return "ChurnBuildResult{" + "x=" + chunkX + ", z=" + chunkZ + ", bytes=" + (data == null ? 0 : data.length) + '}';
    }
}
