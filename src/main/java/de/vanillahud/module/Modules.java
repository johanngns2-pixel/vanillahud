package de.vanillahud.module;

import com.google.gson.*;
import de.vanillahud.setting.Setting;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vanillahud.json");
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();
    public static TextModule PING_MODULE;
    public static Setting.Choice PING_MODE;
    private static final long START = System.currentTimeMillis();

    // combat state
    private static int combo;
    private static long lastHit;
    private static int lastHurt;
    private static double lastReach = -1;
    private static long lastReachTime;
    private static LivingEntity target;
    private static long targetTime;

    private Modules() {}

    // ------------------------------------------------------------------ input stats
    /** Called by the mouse mixin on every click (for CPS). */
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

    public static int cpsLeft() {
        return cps(LEFT);
    }

    public static int cpsRight() {
        return cps(RIGHT);
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static String f(double d, int dec) {
        return String.format(Locale.ROOT, "%." + dec + "f", d);
    }

    public static Module get(String id) {
        for (Module m : ALL) if (m.id.equals(id)) return m;
        return null;
    }

    // ------------------------------------------------------------------ registration helpers
    private static <T extends Module> T reg(T m) {
        ALL.add(m);
        return m;
    }

    private static TextModule text(String id, String name, String desc, Item icon, Category cat, boolean on) {
        return reg(new TextModule(id, name, desc, icon, cat, on));
    }

    // ------------------------------------------------------------------ init
    public static void init() {
        // ---------------- INFO
        TextModule fps = text("fps", "FPS", "Frames per second.", Items.CLOCK, Category.INFO, true);
        Setting.Bool fpsLabel = fps.add(new Setting.Bool("label", "Show Label", true));
        fps.single(mc -> mc.getFps() + (fpsLabel.value ? " FPS" : ""));
        Setting.Bool fpsCol = fps.add(new Setting.Bool("color_value", "Color By Value", false));
        fps.tint = mc -> !fpsCol.value ? null : mc.getFps() >= 60 ? 0xFF55FF55 : mc.getFps() >= 30 ? 0xFFFFFF55 : 0xFFFF5555;

        TextModule cpsM = text("cps", "CPS", "Clicks per second.", Items.IRON_SWORD, Category.PVP, true);
        Setting.Bool cpsRightB = cpsM.add(new Setting.Bool("right", "Show Right Click", true));
        cpsM.single(mc -> cpsRightB.value ? cpsLeft() + " | " + cpsRight() + " CPS" : cpsLeft() + " CPS");

        TextModule coords = text("coords", "Coordinates", "Your XYZ position.", Items.COMPASS, Category.INFO, true);
        Setting.Num cDec = coords.add(new Setting.Num("decimals", "Decimals", 0, 3, 1, 1, ""));
        Setting.Choice cLayout = coords.add(new Setting.Choice("layout", "Layout", 0, "Single Line", "Multi Line"));
        Setting.Bool cDir = coords.add(new Setting.Bool("direction", "Show Direction", false));
        coords.multi(mc -> {
            Player p = mc.player;
            if (p == null) return List.of("-");
            int d = cDec.i();
            List<String> l = new ArrayList<>();
            if (cLayout.is("Single Line")) {
                l.add("XYZ: " + f(p.getX(), d) + " / " + f(p.getY(), d) + " / " + f(p.getZ(), d));
            } else {
                l.add("X: " + f(p.getX(), d));
                l.add("Y: " + f(p.getY(), d));
                l.add("Z: " + f(p.getZ(), d));
            }
            if (cDir.value) l.add("Facing: " + p.getDirection().getName());
            return l;
        });

        TextModule ping = text("ping", "Ping", "Latency to the server.", Items.REDSTONE, Category.INFO, false);

        Setting.Choice pingMode = ping.add(new Setting.Choice("mode", "Update Rate", 0, "Fast (0.25s)", "Normal (1s)", "Server Value"));
        Setting.Bool pingCol = ping.add(new Setting.Bool("color_value", "Color By Value", false));
        Setting.Bool pingLabel = ping.add(new Setting.Bool("label", "Show Label", false));
        ping.tint = mc -> {
            int l = PingTracker.current(mc, pingMode.index == 2);
            if (!pingCol.value || l < 0) return null;
            return l < 80 ? 0xFF55FF55 : l < 160 ? 0xFFFFFF55 : 0xFFFF5555;
        };
        ping.single(mc -> {
            int l = PingTracker.current(mc, pingMode.index == 2);
            if (l < 0) return "-";
            return (pingLabel.value ? "Ping: " : "") + l + " ms";
        });
        PING_MODULE = ping;
        PING_MODE = pingMode;

        TextModule mem = text("memory", "Memory", "Java memory usage.", Items.REDSTONE_TORCH, Category.INFO, false);
        Setting.Bool memPct = mem.add(new Setting.Bool("percent", "Show Percent", false));
        mem.single(mc -> {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) / 1048576L;
            long max = r.maxMemory() / 1048576L;
            return memPct.value ? "RAM: " + (used * 100 / Math.max(1, max)) + "%" : "RAM: " + used + "/" + max + " MB";
        });

        TextModule speed = text("speed", "Speed", "Your movement speed.", Items.SUGAR, Category.INFO, false);
        Setting.Choice speedUnit = speed.add(new Setting.Choice("unit", "Unit", 0, "m/s", "km/h", "Blocks/tick"));
        speed.single(mc -> {
            Player p = mc.player;
            if (p == null) return "-";
            double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
            double bt = Math.sqrt(dx * dx + dz * dz);
            return switch (speedUnit.get()) {
                case "km/h" -> f(bt * 20 * 3.6, 1) + " km/h";
                case "Blocks/tick" -> f(bt, 3) + " b/t";
                default -> f(bt * 20, 2) + " m/s";
            };
        });

        TextModule tps = text("tps", "TPS", "Estimated server ticks per second.", Items.REPEATER, Category.INFO, false);
        Setting.Bool tpsColor = tps.add(new Setting.Bool("color", "Show Lag Warning", true));
        tps.single(mc -> {
            if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) return "TPS: 20.0";
            double v = Tps.get();
            return "TPS: " + f(v, 1) + (tpsColor.value && v < 15 ? " (lag)" : "");
        });

        Setting.Bool tpsCol = tps.add(new Setting.Bool("color_value", "Color By Value", false));
        tps.tint = mc -> !tpsCol.value ? null : Tps.get() >= 18 ? 0xFF55FF55 : Tps.get() >= 15 ? 0xFFFFFF55 : 0xFFFF5555;

        TextModule armorPts = text("armor_points", "Armor Points", "Your armor value.", Items.IRON_CHESTPLATE, Category.INFO, false);
        armorPts.single(mc -> mc.player == null ? "-" : "Armor: " + mc.player.getArmorValue());

        TextModule potions = text("potions", "Potion Status", "Active effects and their time.", Items.POTION, Category.PVP, false);
        Setting.Bool potTime = potions.add(new Setting.Bool("time", "Show Duration", true));
        potions.multi(mc -> {
            Player p = mc.player;
            if (p == null || p.getActiveEffects().isEmpty()) return List.of("No effects");
            List<String> l = new ArrayList<>();
            for (MobEffectInstance e : p.getActiveEffects()) {
                String s = e.getEffect().value().getDisplayName().getString() + " " + (e.getAmplifier() + 1);
                if (potTime.value) {
                    if (e.isInfiniteDuration()) s += " (inf)";
                    else {
                        int sec = e.getDuration() / 20;
                        s += String.format(" (%d:%02d)", sec / 60, sec % 60);
                    }
                }
                l.add(s);
            }
            return l;
        });

        TextModule day = text("day", "Day Counter", "In-game day number.", Items.DAYLIGHT_DETECTOR, Category.INFO, false);
        day.single(mc -> mc.level == null ? "-" : "Day " + (mc.level.getDayTime() / 24000L));

        TextModule clock = text("clock", "Clock", "Real time and date.", Items.CLOCK, Category.INFO, false);
        Setting.Bool c24 = clock.add(new Setting.Bool("h24", "24 Hour Format", true));
        Setting.Bool cSec = clock.add(new Setting.Bool("seconds", "Show Seconds", true));
        Setting.Bool cDate = clock.add(new Setting.Bool("date", "Show Date", false));
        clock.single(mc -> {
            String pat = (c24.value ? "HH:mm" : "hh:mm") + (cSec.value ? ":ss" : "") + (c24.value ? "" : " a");
            if (cDate.value) pat = "dd.MM.yyyy  " + pat;
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pat, Locale.ENGLISH));
        });

        TextModule ip = text("server_ip", "Server IP", "Address of the server.", Items.NAME_TAG, Category.INFO, false);
        Setting.Bool ipPort = ip.add(new Setting.Bool("hide_port", "Hide Port", false));
        Setting.Bool ipHide = ip.add(new Setting.Bool("hide", "Hide Address (Streamer Mode)", false));
        ip.single(mc -> {
            var sv = mc.getCurrentServer();
            if (sv == null) return "Singleplayer";
            if (ipHide.value) return "Server: ********";
            String a = sv.ip;
            if (ipPort.value && a.contains(":")) a = a.substring(0, a.lastIndexOf(':'));
            return a;
        });

        TextModule players = text("players", "Players Online", "Number of players on the server.", Items.PLAYER_HEAD, Category.INFO, false);
        players.single(mc -> mc.getConnection() == null ? "-" : "Players: " + mc.getConnection().getOnlinePlayers().size());

        TextModule xp = text("xp", "XP Level", "Your experience level.", Items.EXPERIENCE_BOTTLE, Category.INFO, false);
        xp.single(mc -> mc.player == null ? "-" : "Level " + mc.player.experienceLevel);

        TextModule dir = text("direction", "Direction", "The way you are facing.", Items.MAP, Category.INFO, false);
        Setting.Bool dirShort = dir.add(new Setting.Bool("short", "Short Names", false));
        Setting.Bool dirAxis = dir.add(new Setting.Bool("axis", "Show Axis", true));
        dir.single(mc -> {
            if (mc.player == null) return "-";
            var d = mc.player.getDirection();
            String n = d.getName();
            String name = dirShort.value ? n.substring(0, 1).toUpperCase() : Character.toUpperCase(n.charAt(0)) + n.substring(1);
            String axis = switch (d) {
                case NORTH -> "-Z";
                case SOUTH -> "+Z";
                case EAST -> "+X";
                case WEST -> "-X";
                default -> "";
            };
            return "Facing: " + name + (dirAxis.value && !axis.isEmpty() ? " (" + axis + ")" : "");
        });

        TextModule angle = text("angle", "Angle Display", "Yaw and pitch of your camera.", Items.SPYGLASS, Category.INFO, false);
        Setting.Num aDec = angle.add(new Setting.Num("decimals", "Decimals", 0, 3, 1, 1, ""));
        Setting.Choice aLayout = angle.add(new Setting.Choice("layout", "Layout", 0, "Single Line", "Multi Line"));
        angle.multi(mc -> {
            Player p = mc.player;
            if (p == null) return List.of("-");
            double yaw = ((p.getYRot() % 360) + 360) % 360;
            if (yaw > 180) yaw -= 360;
            if (aLayout.is("Single Line")) return List.of("Yaw: " + f(yaw, aDec.i()) + "  Pitch: " + f(p.getXRot(), aDec.i()));
            return List.of("Yaw: " + f(yaw, aDec.i()), "Pitch: " + f(p.getXRot(), aDec.i()));
        });

        TextModule biome = text("biome", "Biome", "The biome you are in.", Items.OAK_SAPLING, Category.INFO, false);
        biome.single(mc -> {
            if (mc.level == null || mc.player == null) return "-";
            String n = mc.level.getBiome(mc.player.blockPosition()).getRegisteredName();
            if (n.contains(":")) n = n.substring(n.indexOf(':') + 1);
            StringBuilder sb = new StringBuilder("Biome: ");
            for (String part : n.split("_")) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
            return sb.toString().trim();
        });

        TextModule light = text("light", "Light Level", "Block and sky light at your position.", Items.TORCH, Category.INFO, false);
        light.single(mc -> {
            if (mc.level == null || mc.player == null) return "-";
            var pos = mc.player.blockPosition();
            return "Light: " + mc.level.getBrightness(LightLayer.BLOCK, pos) + " block / " + mc.level.getBrightness(LightLayer.SKY, pos) + " sky";
        });

        TextModule health = text("health", "Health", "Your health points.", Items.GOLDEN_APPLE, Category.INFO, false);
        Setting.Bool hAbs = health.add(new Setting.Bool("absorption", "Include Absorption", true));
        health.single(mc -> {
            Player p = mc.player;
            if (p == null) return "-";
            double v = p.getHealth() + (hAbs.value ? p.getAbsorptionAmount() : 0);
            return "Health: " + f(v, 1);
        });

        Setting.Bool hpCol = health.add(new Setting.Bool("color_value", "Color By Value", false));
        health.tint = mc -> {
            if (!hpCol.value || mc.player == null) return null;
            float hp = mc.player.getHealth();
            return hp >= 14 ? 0xFF55FF55 : hp >= 7 ? 0xFFFFFF55 : 0xFFFF5555;
        };

        TextModule hunger = text("hunger", "Hunger", "Food and saturation level.", Items.COOKED_BEEF, Category.INFO, false);
        hunger.single(mc -> mc.player == null ? "-" : "Food: " + mc.player.getFoodData().getFoodLevel() + "  Sat: " + f(mc.player.getFoodData().getSaturationLevel(), 1));

        TextModule session = text("session", "Session Timer", "How long you have been playing.", Items.RECOVERY_COMPASS, Category.INFO, false);
        session.single(mc -> {
            long s = (System.currentTimeMillis() - START) / 1000;
            return String.format("Playtime: %d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
        });

        TextModule held = text("held_item", "Held Item", "Name and durability of your held item.", Items.IRON_PICKAXE, Category.INFO, false);
        held.multi(mc -> {
            if (mc.player == null) return List.of("-");
            ItemStack st = mc.player.getMainHandItem();
            if (st.isEmpty()) return List.of("Empty hand");
            List<String> l = new ArrayList<>();
            l.add(st.getHoverName().getString());
            if (st.isDamageableItem()) l.add("Durability: " + (st.getMaxDamage() - st.getDamageValue()) + "/" + st.getMaxDamage());
            else if (st.getCount() > 1) l.add("Count: " + st.getCount());
            return l;
        });

        // ---------------- PVP
        TextModule reach = text("reach", "Reach Display", "Distance of your last hit.", Items.STICK, Category.PVP, false);
        Setting.Num reachHold = reach.add(new Setting.Num("hold", "Hold Time", 1, 15, 1, 5, "s"));
        Setting.Num reachDec = reach.add(new Setting.Num("decimals", "Decimals", 0, 3, 1, 2, ""));
        reach.single(mc -> {
            if (lastReach < 0 || System.currentTimeMillis() - lastReachTime > reachHold.i() * 1000L) return "Reach: -";
            return "Reach: " + f(lastReach, reachDec.i());
        });

        TextModule comboM = text("combo", "Combo Counter", "Hits in a row without taking damage.", Items.BLAZE_ROD, Category.PVP, false);
        Setting.Num comboTime = comboM.add(new Setting.Num("timeout", "Reset After", 1, 10, 1, 3, "s"));
        comboM.single(mc -> {
            if (System.currentTimeMillis() - lastHit > comboTime.i() * 1000L) combo = 0;
            return combo + " Combo";
        });

        TextModule targetM = text("target_info", "Target Info", "Name and health of the last entity you hit or looked at.", Items.SKELETON_SKULL, Category.PVP, false);
        Setting.Num targetHold = targetM.add(new Setting.Num("hold", "Hold Time", 1, 15, 1, 4, "s"));
        targetM.multi(mc -> {
            if (mc.crosshairPickEntity instanceof LivingEntity le && mc.player != null && le != mc.player) {
                target = le;
                targetTime = System.currentTimeMillis();
            }
            if (target == null || target.isRemoved() || System.currentTimeMillis() - targetTime > targetHold.i() * 1000L) return List.of("No target");
            double dist = mc.player == null ? 0 : mc.player.distanceTo(target);
            return List.of(target.getName().getString(), "HP: " + f(target.getHealth(), 1) + "/" + f(target.getMaxHealth(), 1), "Distance: " + f(dist, 1));
        });

        reg(new KeystrokesModule());
        reg(new ArmorModule());
        reg(new ItemCounterModule());
        reg(new InventoryModule());
        reg(new Features.HitColor());

        // ---------------- VISUAL
        reg(new Features.Crosshair());
        reg(new Features.BlockOutline());
        reg(new Features.NametagPing());
        reg(new Features.Particles());
        reg(new Features.Fullbright());
        reg(new Features.NoFog());
        reg(new Features.Weather());
        reg(new Features.NoBobbing());
        reg(new Features.OptionScale("no_hurt_cam", "No Hurt Cam", "Removes the camera tilt when you take damage.", Items.LEATHER_HELMET, () -> mc().options.damageTiltStrength(), 0.0));
        reg(new Features.OptionScale("static_fov", "Static FOV", "No FOV change from sprinting or speed.", Items.FEATHER, () -> mc().options.fovEffectScale(), 0.0));
        reg(new Features.OptionScale("no_darkness", "No Darkness Pulse", "Disables the darkness effect pulsing.", Items.SCULK_SENSOR, () -> mc().options.darknessEffectScale(), 0.0));
        reg(new Features.OptionScale("no_distortion", "No Distortion", "Disables nausea and portal wobble.", Items.POISONOUS_POTATO, () -> mc().options.screenEffectScale(), 0.0));
        reg(new Features.HudCleaner());

        // ---------------- UTILITY
        reg(new SprintModule());
        reg(new Features.F3Menu());
        reg(new ZoomModule());

        // default positions for HUD modules
        int i = 0;
        for (Module m : ALL) {
            if (m instanceof HudModule h) {
                h.x = 4 + (i / 12) * 140;
                h.y = 4 + (i % 12) * 22;
                i++;
            }
        }

        // combat tracking
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide() && player == mc().player && entity instanceof LivingEntity) {
                long now = System.currentTimeMillis();
                combo = now - lastHit > 3000 ? 1 : combo + 1;
                lastHit = now;
                lastReach = Math.sqrt(entity.getBoundingBox().distanceToSqr(player.getEyePosition()));
                lastReachTime = now;
            }
            return InteractionResult.PASS;
        });

        load();
    }

    public static void tick(Minecraft mc) {
        if (mc.player != null) {
            int h = mc.player.hurtTime;
            if (h > lastHurt) combo = 0;
            lastHurt = h;
        }
        boolean needPing = (PING_MODULE != null && PING_MODULE.enabled && PING_MODE.index != 2)
                || (Features.NametagPing.INSTANCE != null && Features.NametagPing.INSTANCE.enabled && Features.NametagPing.INSTANCE.live.value);
        if (needPing) PingTracker.tick(mc, PING_MODULE != null && PING_MODULE.enabled && PING_MODE.index == 1 ? 1000 : 250);
        for (Module m : ALL) {
            if (!m.enabled) continue;
            try {
                m.onTick(mc);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void shutdown() {
        for (Module m : ALL) {
            if (!m.enabled) continue;
            try {
                m.onDisable();
            } catch (Throwable ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ order
    public static void move(Module m, int dir) {
        int i = ALL.indexOf(m);
        int j = i + dir;
        if (i < 0 || j < 0 || j >= ALL.size()) return;
        Collections.swap(ALL, i, j);
        save();
    }

    // ------------------------------------------------------------------ config
    public static void save() {
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        JsonArray order = new JsonArray();
        for (Module m : ALL) {
            order.add(m.id);
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.enabled);
            if (m instanceof HudModule h) {
                o.addProperty("x", h.x);
                o.addProperty("y", h.y);
            }
            JsonObject s = new JsonObject();
            for (Setting st : m.settings) {
                JsonElement e = st.save();
                if (!(st instanceof Setting.Action) && e != null) s.add(st.id, e);
            }
            o.add("settings", s);
            mods.add(m.id, o);
        }
        root.add("order", order);
        root.add("modules", mods);
        try {
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {
        }
    }

    private static void load() {
        try {
            if (!Files.exists(FILE)) return;
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : ALL) {
                if (!mods.has(m.id)) continue;
                JsonObject o = mods.getAsJsonObject(m.id);
                if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean();
                if (m instanceof HudModule h) {
                    if (o.has("x")) h.x = o.get("x").getAsInt();
                    if (o.has("y")) h.y = o.get("y").getAsInt();
                }
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting st : m.settings) {
                        if (!s.has(st.id)) continue;
                        try {
                            st.load(s.get(st.id));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            if (root.has("order")) {
                List<Module> sorted = new ArrayList<>();
                for (JsonElement e : root.getAsJsonArray("order")) {
                    Module m = get(e.getAsString());
                    if (m != null && !sorted.contains(m)) sorted.add(m);
                }
                for (Module m : ALL) if (!sorted.contains(m)) sorted.add(m);
                ALL.clear();
                ALL.addAll(sorted);
            }
        } catch (Exception ignored) {
        }
    }
}
