package de.vanillahud.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;

/** Measures the real round trip time to the server with ping packets (much faster than the tab list value). */
public final class PingTracker {
    private static long lastSend;
    private static double smooth = -1;

    private PingTracker() {}

    public static void tick(Minecraft mc, int intervalMs) {
        if (mc.getConnection() == null || mc.player == null) {
            smooth = -1;
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSend >= intervalMs) {
            lastSend = now;
            try {
                mc.getConnection().send(new ServerboundPingRequestPacket(now));
            } catch (Throwable ignored) {
            }
        }
    }

    public static void onPong(long sentTime) {
        long rtt = System.currentTimeMillis() - sentTime;
        if (rtt < 0 || rtt > 30000) return;
        smooth = smooth < 0 ? rtt : smooth * 0.5 + rtt * 0.5;
    }

    /** Live ping in ms, or the tab list value when serverValue is set or no live data exists yet. */
    public static int current(Minecraft mc, boolean serverValue) {
        if (!serverValue && smooth >= 0) return (int) Math.round(smooth);
        return tabList(mc);
    }

    public static int tabList(Minecraft mc) {
        if (mc.getConnection() == null || mc.player == null) return -1;
        var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    public static int live() {
        return smooth < 0 ? -1 : (int) Math.round(smooth);
    }
}
