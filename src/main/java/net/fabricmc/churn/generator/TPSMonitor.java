package net.fabricmc.churn.generator;

public class TPSMonitor {
    private static final TPSMonitor INSTANCE = new TPSMonitor();
    private volatile double tps = 20.0;
    private long lastTick = System.currentTimeMillis();
    private double ewma = 20.0;
    private final double alpha = 0.15;

    public static TPSMonitor getInstance() { return INSTANCE; }

    public synchronized void recordTick() {
        long now = System.currentTimeMillis();
        long dt = now - lastTick;
        if (dt <= 0) return;
        double inst = 1000.0 / dt;
        ewma = alpha * inst + (1 - alpha) * ewma;
        tps = ewma;
        lastTick = now;
    }

    public double getTps() { return tps; }
}
