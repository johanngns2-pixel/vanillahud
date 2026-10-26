package de.vanillahud.module;

/** Estimates the server TPS from the time update packets. */
public final class Tps {
    private static long lastGameTime = -1;
    private static long lastNanos;
    private static double tps = 20.0;

    private Tps() {}

    public static void onTime(long gameTime) {
        long now = System.nanoTime();
        if (lastGameTime >= 0 && gameTime > lastGameTime && now > lastNanos) {
            double sample = (gameTime - lastGameTime) * 1e9 / (now - lastNanos);
            sample = Math.max(0, Math.min(20, sample));
            tps = tps * 0.6 + sample * 0.4;
        }
        lastGameTime = gameTime;
        lastNanos = now;
    }

    public static void reset() {
        lastGameTime = -1;
        tps = 20.0;
    }

    public static double get() {
        return tps;
    }
}
