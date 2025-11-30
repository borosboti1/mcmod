package net.fabricmc.churn.generator;

/**
 * Represents a chunk position task relative to an origin.
 */
public class ChurnTask {
    public final int chunkX;
    public final int chunkZ;

    public ChurnTask(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @Override
    public String toString() {
        return "ChurnTask{" + "x=" + chunkX + ", z=" + chunkZ + '}';
    }
}
