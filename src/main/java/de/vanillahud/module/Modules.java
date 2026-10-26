package de.vanillahud.module;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

public final class Modules {
    public static final List<HudModule> ALL = new ArrayList<>();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vanillahud.json");
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    private Modules() {}

    /** Wird vom Mixin bei jedem Mausklick aufgerufen (fuer CPS). */
    public static void click(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) LEFT.add(now);
        else if (button == 1) RIGHT.add(now);
    }

    private static int cps(Deque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static void add(String id, String name, Item icon, int y, boolean on, Supplier<String> s) {
        ALL.add(new HudModule(id, name, icon, 4, y, on, s));
    }

    public static void init() {
        add("fps", "FPS", Items.CLOCK, 4, true, () -> mc().getFps() + " FPS");
        add("cps", "CPS", Items.IRON_SWORD, 22, true, () -> cps(LEFT) + " | " + cps(RIGHT) + " CPS");
        add("coords", "Koordinaten", Items.COMPASS, 40, true, () -> {
            Player p = mc().player;
            if (p == null) return "-";
            return String.format("XYZ: %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ());
        });
        add("ping", "Ping", Items.REDSTONE, 58, false, () -> {
            var c = mc().getConnection();
            if (c == null || mc().player == null) return "-";
            var info = c.getPlayerInfo(mc().player.getUUID());
            return (info == null ? 0 : info.getLatency()) + " ms";
        });
        add("memory", "Speicher", Items.REDSTONE_TORCH, 76, false, () -> {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) / 1048576L;
            long max = r.maxMemory() / 1048576L;
            return "RAM: " + used + "/" + max + " MB";
        });
        add("speed", "Geschwindigkeit", Items.SUGAR, 94, false, () -> {
            Player p = mc().player;
            if (p == null) return "-";
            double dx = p.getX() - p.xo;
            double dz = p.getZ() - p.zo;
            return String.format("%.2f m/s", Math.sqrt(dx * dx + dz * dz) * 20);
        });
        add("armor", "Ruestung", Items.IRON_CHESTPLATE, 112, false, () -> {
            Player p = mc().player;
            return p == null ? "-" : "Ruestung: " + p.getArmorValue();
        });
        add("potions", "Traenke", Items.POTION, 130, false, () -> {
            Player p = mc().player;
            if (p == null || p.getActiveEffects().isEmpty()) return "Keine Effekte";
            StringBuilder sb = new StringBuilder();
            for (MobEffectInstance e : p.getActiveEffects()) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(e.getEffect().value().getDisplayName().getString()).append(' ').append(e.getAmplifier() + 1);
            }
            return sb.toString();
        });
        add("day", "Tageszaehler", Items.DAYLIGHT_DETECTOR, 148, false,
                () -> mc().level == null ? "-" : "Tag " + (mc().level.getDayTime() / 24000L));
        add("time", "Echtzeit", Items.CLOCK, 166, false,
                () -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        add("ip", "Server-IP", Items.NAME_TAG, 184, false, () -> {
            var s = mc().getCurrentServer();
            return s == null ? "Singleplayer" : s.ip;
        });
        add("xp", "XP", Items.EXPERIENCE_BOTTLE, 202, false, () -> {
            Player p = mc().player;
            return p == null ? "-" : "Level " + p.experienceLevel;
        });
        add("direction", "Richtung", Items.MAP, 220, false, () -> {
            Player p = mc().player;
            return p == null ? "-" : p.getDirection().getName();
        });
        load();
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (HudModule m : ALL) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.enabled);
            o.addProperty("x", m.x);
            o.addProperty("y", m.y);
            root.add(m.id, o);
        }
        try {
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {
        }
    }

    private static void load() {
        try {
            if (!Files.exists(FILE)) return;
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            for (HudModule m : ALL) {
                if (!root.has(m.id)) continue;
                JsonObject o = root.getAsJsonObject(m.id);
                m.enabled = o.get("enabled").getAsBoolean();
                m.x = o.get("x").getAsInt();
                m.y = o.get("y").getAsInt();
            }
        } catch (Exception ignored) {
        }
    }
}
