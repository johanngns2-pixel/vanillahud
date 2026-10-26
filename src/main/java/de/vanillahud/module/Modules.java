package de.vanillahud.module;

import com.google.gson.*;
import de.vanillahud.mixin.OverlayTextureAccessor;
import de.vanillahud.module.impl.ArmorStatusModule;
import de.vanillahud.module.impl.KeystrokesModule;
import de.vanillahud.setting.*;
import de.vanillahud.util.I18n;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
    private static final long START = System.currentTimeMillis();

    public static KeyMapping zoomKey;
    private static float zoomCur = 1f;
    private static int combo, lastHurt;
    private static double lastReach = -1;
    private static boolean sprintForced, sprintActive;
    private static int appliedHit = -1;

    private Modules() {}

    // ---------------- Eingaben / Events ----------------

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

    /** Vom GameRenderer-Mixin gelesen: Faktor, durch den das Sichtfeld geteilt wird. */
    public static float zoomFactor() {
        return zoomCur;
    }

    public static void tick(Minecraft mc) {
        for (HudModule m : ALL) if (m.enabled) m.onTick(mc);
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static Player pl() {
        return mc().player;
    }

    // ---------------- Registrierung ----------------

    private static HudModule add(String id, Item icon, int x, int y, boolean on) {
        HudModule m = new HudModule(id, icon, x, y, on, true);
        ALL.add(m);
        return m;
    }

    private static HudModule text(String id, Item icon, int x, int y, boolean on, Supplier<String> s) {
        HudModule m = add(id, icon, x, y, on);
        m.text = s;
        return m;
    }

    private static HudModule tool(String id, Item icon) {
        HudModule m = new HudModule(id, icon, 0, 0, false, false);
        ALL.add(m);
        return m;
    }

    private static int count(Item item) {
        Player p = pl();
        if (p == null) return 0;
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() == item) n += s.getCount();
        }
        return n;
    }

    private static void counter(String id, Item item, int y) {
        text(id, item, 130, y, false, () -> I18n.tr("hud." + id, count(item)));
    }

    public static void init() {
        // ----- bestehende Module -----
        text("fps", Items.CLOCK, 4, 4, true, () -> mc().getFps() + " FPS");
        text("cps", Items.IRON_SWORD, 4, 22, true, () -> cps(LEFT) + " | " + cps(RIGHT) + " CPS");

        HudModule coords = add("coords", Items.COMPASS, 4, 40, true);
        IntSetting dec = coords.add(new IntSetting("decimals", 0, 3, 1, ""));
        coords.text = () -> {
            Player p = pl();
            if (p == null) return "-";
            String f = "%." + dec.value + "f";
            return String.format("XYZ: " + f + " / " + f + " / " + f, p.getX(), p.getY(), p.getZ());
        };

        text("ping", Items.REDSTONE, 4, 58, false, () -> {
            var c = mc().getConnection();
            if (c == null || pl() == null) return "-";
            var info = c.getPlayerInfo(pl().getUUID());
            return (info == null ? 0 : info.getLatency()) + " ms";
        });
        text("memory", Items.REDSTONE_TORCH, 4, 76, false, () -> {
            Runtime r = Runtime.getRuntime();
            return I18n.tr("hud.ram", (r.totalMemory() - r.freeMemory()) / 1048576L, r.maxMemory() / 1048576L);
        });

        HudModule speed = add("speed", Items.SUGAR, 4, 94, false);
        EnumSetting unit = speed.add(new EnumSetting("speedUnit", 0, "ms", "kmh"));
        speed.text = () -> {
            Player p = pl();
            if (p == null) return "-";
            double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
            double v = Math.sqrt(dx * dx + dz * dz) * 20;
            return unit.get().equals("kmh") ? String.format("%.1f km/h", v * 3.6) : String.format("%.2f m/s", v);
        };

        text("armor", Items.IRON_CHESTPLATE, 4, 112, false,
                () -> pl() == null ? "-" : I18n.tr("hud.armor", pl().getArmorValue()));
        text("potions", Items.POTION, 4, 130, false, () -> {
            Player p = pl();
            if (p == null || p.getActiveEffects().isEmpty()) return I18n.tr("hud.noEffects");
            StringBuilder sb = new StringBuilder();
            for (MobEffectInstance e : p.getActiveEffects()) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(e.getEffect().value().getDisplayName().getString()).append(' ').append(e.getAmplifier() + 1);
            }
            return sb.toString();
        });
        text("day", Items.DAYLIGHT_DETECTOR, 4, 148, false,
                () -> mc().level == null ? "-" : I18n.tr("hud.day", mc().level.getDayTime() / 24000L));

        HudModule time = add("time", Items.CLOCK, 4, 166, false);
        BoolSetting h24 = time.add(new BoolSetting("use24h", true));
        BoolSetting secs = time.add(new BoolSetting("seconds", true));
        time.text = () -> LocalTime.now().format(DateTimeFormatter.ofPattern(
                (h24.value ? "HH" : "hh") + ":mm" + (secs.value ? ":ss" : "") + (h24.value ? "" : " a")));

        text("ip", Items.NAME_TAG, 4, 184, false, () -> {
            var s = mc().getCurrentServer();
            return s == null ? I18n.tr("hud.singleplayer") : s.ip;
        });
        text("xp", Items.EXPERIENCE_BOTTLE, 4, 202, false,
                () -> pl() == null ? "-" : I18n.tr("hud.level", pl().experienceLevel));
        text("direction", Items.MAP, 4, 220, false, () -> pl() == null ? "-" : pl().getDirection().getName());

        // ----- neue HUD-Module -----
        ALL.add(new KeystrokesModule(4, 80, true));
        ALL.add(new ArmorStatusModule(4, 170, true));

        text("health", Items.GOLDEN_APPLE, 130, 4, false, () -> {
            Player p = pl();
            if (p == null) return "-";
            float abs = p.getAbsorptionAmount();
            return I18n.tr("hud.health", String.format("%.1f/%.0f", p.getHealth(), p.getMaxHealth())
                    + (abs > 0 ? String.format(" +%.1f", abs) : ""));
        });
        text("hunger", Items.COOKED_BEEF, 130, 22, false, () -> {
            Player p = pl();
            return p == null ? "-" : I18n.tr("hud.food", p.getFoodData().getFoodLevel(),
                    String.format("%.1f", p.getFoodData().getSaturationLevel()));
        });
        text("combo", Items.DIAMOND_SWORD, 130, 40, false, () -> I18n.tr("hud.combo", combo));
        text("reach", Items.SPYGLASS, 130, 58, false,
                () -> I18n.tr("hud.reach", lastReach < 0 ? "-" : String.format("%.2f", lastReach)));
        text("cooldown", Items.NETHERITE_SWORD, 130, 76, false,
                () -> pl() == null ? "-" : I18n.tr("hud.cooldown", Math.round(pl().getAttackStrengthScale(0f) * 100)));
        text("light", Items.TORCH, 130, 94, false, () -> {
            if (mc().level == null || pl() == null) return "-";
            return I18n.tr("hud.light", mc().level.getMaxLocalRawBrightness(pl().blockPosition()));
        });
        text("rotation", Items.ENDER_EYE, 130, 112, false, () -> {
            Player p = pl();
            return p == null ? "-" : I18n.tr("hud.rotation", Mth.wrapDegrees(p.getYRot()), p.getXRot());
        });
        text("target", Items.SPYGLASS, 130, 130, false, () -> {
            Minecraft m = mc();
            if (m.level == null) return "-";
            if (m.crosshairPickEntity != null) return I18n.tr("hud.target", m.crosshairPickEntity.getName().getString());
            if (m.hitResult instanceof BlockHitResult b && m.hitResult.getType() == HitResult.Type.BLOCK) {
                return I18n.tr("hud.target", m.level.getBlockState(b.getBlockPos()).getBlock().getName().getString());
            }
            return "";
        });
        text("worldtime", Items.DAYLIGHT_DETECTOR, 130, 148, false, () -> {
            if (mc().level == null) return "-";
            long t = mc().level.getDayTime() % 24000L;
            return String.format("%02d:%02d", (t / 1000 + 6) % 24, (t % 1000) * 60 / 1000);
        });
        text("session", Items.CLOCK, 130, 166, false, () -> {
            long s = (System.currentTimeMillis() - START) / 1000;
            return I18n.tr("hud.session", String.format("%02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60));
        });
        text("players", Items.PLAYER_HEAD, 130, 184, false, () -> {
            var c = mc().getConnection();
            return c == null ? "-" : I18n.tr("hud.players", c.getOnlinePlayers().size());
        });
        text("entities", Items.ZOMBIE_HEAD, 130, 202, false,
                () -> mc().level == null ? "-" : I18n.tr("hud.entities", mc().level.getEntityCount()));
        text("freeslots", Items.CHEST, 130, 220, false, () -> {
            Player p = pl();
            if (p == null) return "-";
            int n = 0;
            for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (s.isEmpty()) n++;
            return I18n.tr("hud.freeslots", n);
        });
        text("itemcount", Items.HOPPER, 130, 238, false, () -> {
            Player p = pl();
            if (p == null) return "-";
            ItemStack s = p.getInventory().getSelectedItem();
            return s.isEmpty() ? "" : I18n.tr("hud.itemcount", s.getHoverName().getString(), count(s.getItem()));
        });
        counter("gapples", Items.GOLDEN_APPLE, 256);
        counter("pearls", Items.ENDER_PEARL, 274);
        counter("totems", Items.TOTEM_OF_UNDYING, 292);
        counter("arrows", Items.ARROW, 310);

        // ----- Werkzeug-Module (kein HUD-Kasten, nur Einstellungen) -----
        HudModule sprint = add("togglesprint", Items.LEATHER_BOOTS, 4, 260, false);
        sprint.text = () -> sprintActive ? I18n.tr("hud.sprint") : "";
        sprint.tick = m -> {
            Player p = m.player;
            sprintActive = p != null && m.screen == null && m.options.keyUp.isDown() && !p.isShiftKeyDown();
            if (sprintActive) {
                m.options.keySprint.setDown(true);
                sprintForced = true;
            } else if (sprintForced) {
                m.options.keySprint.setDown(false);
                sprintForced = false;
            }
        };
        sprint.onDisable = () -> {
            if (sprintForced) mc().options.keySprint.setDown(false);
            sprintForced = false;
            sprintActive = false;
        };

        HudModule fb = tool("fullbright", Items.GLOWSTONE);
        boolean[] fbApplied = {false};
        fb.tick = m -> {
            Player p = m.player;
            if (p == null) return;
            if (!p.hasEffect(MobEffects.NIGHT_VISION)) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100000, 0, false, false, false));
                fbApplied[0] = true;
            }
        };
        fb.onDisable = () -> {
            Player p = pl();
            if (p != null && fbApplied[0]) p.removeEffect(MobEffects.NIGHT_VISION);
            fbApplied[0] = false;
        };

        HudModule zoom = tool("zoom", Items.SPYGLASS);
        IntSetting zf = zoom.add(new IntSetting("zoomFactor", 2, 10, 4, "x"));
        zoom.tick = m -> {
            float target = (zoomKey != null && zoomKey.isDown() && m.screen == null) ? zf.value : 1f;
            zoomCur += (target - zoomCur) * 0.4f;
            if (Math.abs(target - zoomCur) < 0.01f) zoomCur = target;
        };
        zoom.onDisable = () -> zoomCur = 1f;

        HudModule hit = tool("hitcolor", Items.RED_DYE);
        ColorSetting hc = hit.add(new ColorSetting("hitColor", 0xFF0000));
        IntSetting ha = hit.add(new IntSetting("hitAlpha", 0, 100, 70, "%"));
        hit.tick = m -> applyHitColor(m, hc.value, ha.value);
        hit.onDisable = () -> applyHitColor(mc(), 0xFF0000, 70);

        // ----- Kampf-Tracking (Combo / Reach) -----
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player == mc().player && entity instanceof LivingEntity) {
                combo++;
                AABB b = entity.getBoundingBox();
                Vec3 e = player.getEyePosition();
                double dx = Mth.clamp(e.x, b.minX, b.maxX) - e.x;
                double dy = Mth.clamp(e.y, b.minY, b.maxY) - e.y;
                double dz = Mth.clamp(e.z, b.minZ, b.maxZ) - e.z;
                lastReach = Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
            return InteractionResult.PASS;
        });
        load();
    }

    /** Combo zuruecksetzen, sobald man selbst getroffen wird. */
    public static void trackCombat(Minecraft mc) {
        Player p = mc.player;
        if (p == null) return;
        if (p.hurtTime > 0 && lastHurt == 0) combo = 0;
        lastHurt = p.hurtTime;
    }

    /** Die rote Treffer-Ueberlagerung ist eine Textur (Zeilen 0-7): einfach umfaerben. */
    private static void applyHitColor(Minecraft m, int rgb, int alphaPct) {
        int key = (rgb << 8) | alphaPct;
        if (key == appliedHit) return;
        try {
            var tex = ((OverlayTextureAccessor) m.gameRenderer.overlayTexture()).vh$texture();
            var img = tex.getPixels();
            if (img == null) return;
            int a = Math.round(alphaPct * 255 / 100f);
            img.fillRect(0, 0, 16, 8, (a << 24) | rgb);
            tex.upload();
            appliedHit = key;
        } catch (Throwable ignored) {
        }
    }

    // ---------------- Speichern / Laden ----------------

    public static void save() {
        JsonObject root = new JsonObject();
        JsonObject global = new JsonObject();
        global.addProperty("language", I18n.lang);
        root.add("_global", global);
        for (HudModule m : ALL) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.enabled);
            o.addProperty("x", m.x);
            o.addProperty("y", m.y);
            JsonObject s = new JsonObject();
            for (Setting st : m.settings) s.add(st.id, st.save());
            o.add("settings", s);
            root.add(m.id, o);
        }
        try {
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {
        }
    }

    private static void load() {
        // Minecraft-Optionen gibt es beim Mod-Start noch nicht -> Systemsprache nehmen
        String def = java.util.Locale.getDefault().getLanguage().equals("de") ? "de" : "en";
        I18n.setLang(def);
        try {
            if (!Files.exists(FILE)) return;
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            if (root.has("_global")) {
                JsonObject g = root.getAsJsonObject("_global");
                if (g.has("language")) I18n.setLang(g.get("language").getAsString());
            }
            for (HudModule m : ALL) {
                if (!root.has(m.id)) continue;
                JsonObject o = root.getAsJsonObject(m.id);
                if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean();
                if (o.has("x")) m.x = o.get("x").getAsInt();
                if (o.has("y")) m.y = o.get("y").getAsInt();
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting st : m.settings) {
                        try {
                            if (s.has(st.id)) st.load(s.get(st.id));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
