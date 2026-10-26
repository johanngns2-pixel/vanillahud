package de.vanillahud.module;

import com.mojang.blaze3d.platform.NativeImage;
import de.vanillahud.mixin.FogRendererAccessor;
import de.vanillahud.mixin.OverlayTextureAccessor;
import de.vanillahud.setting.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/** All modules that are not HUD boxes. */
public final class Features {
    private Features() {}

    // ------------------------------------------------------------------ Fullbright
    public static class Fullbright extends Module {
        public Fullbright() {
            super("fullbright", "Fullbright", "Permanent night vision effect (client side).", Items.GLOWSTONE, Category.VISUAL, false);
        }

        @Override
        public void onTick(Minecraft mc) {
            Player p = mc.player;
            if (p == null) return;
            MobEffectInstance cur = p.getEffect(MobEffects.NIGHT_VISION);
            if (cur == null || cur.getDuration() < 400 && !cur.isInfiniteDuration()) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100000, 0, false, false, false));
            }
        }

        @Override
        public void onDisable() {
            Player p = Minecraft.getInstance().player;
            if (p != null) p.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    // ------------------------------------------------------------------ Option scalers
    /** Sets a double option (0..1 scale options) to a fixed value while enabled. */
    public static class OptionScale extends Module {
        private final Supplier<OptionInstance<Double>> option;
        private final double value;
        private Double old;

        public OptionScale(String id, String name, String desc, net.minecraft.world.item.Item icon, Supplier<OptionInstance<Double>> option, double value) {
            super(id, name, desc, icon, Category.VISUAL, false);
            this.option = option;
            this.value = value;
        }

        @Override
        public void onEnable() {
            if (Minecraft.getInstance().options == null) return;
            OptionInstance<Double> o = option.get();
            if (old == null) old = o.get();
            o.set(value);
        }

        @Override
        public void onTick(Minecraft mc) {
            OptionInstance<Double> o = option.get();
            if (old == null) old = o.get();
            if (o.get() != value) o.set(value);
        }

        @Override
        public void onDisable() {
            if (old != null) option.get().set(old);
            old = null;
        }
    }

    public static class NoBobbing extends Module {
        private Boolean old;

        public NoBobbing() {
            super("no_bobbing", "No View Bobbing", "Disables the walking camera bobbing.", Items.LEAD, Category.VISUAL, false);
        }

        @Override
        public void onTick(Minecraft mc) {
            OptionInstance<Boolean> o = mc.options.bobView();
            if (old == null) old = o.get();
            if (o.get()) o.set(false);
        }

        @Override
        public void onDisable() {
            if (old != null) Minecraft.getInstance().options.bobView().set(old);
            old = null;
        }
    }

    // ------------------------------------------------------------------ Hit color
    public static class HitColor extends Module {
        public final Setting.Color color = add(new Setting.Color("color", "Hit Color", 0xFFFF0000, false, false));
        public final Setting.Num intensity = add(new Setting.Num("intensity", "Intensity", 0, 100, 5, 45, "%"));
        private int applied = 0;

        public HitColor() {
            super("hit_color", "Hit Color", "Changes the color of the damage flash on entities.", Items.REDSTONE, Category.PVP, false);
        }

        private static void apply(int overlayArgb) {
            try {
                OverlayTexture ot = Minecraft.getInstance().gameRenderer.overlayTexture();
                DynamicTexture tex = ((OverlayTextureAccessor) ot).vanillahud$getTexture();
                NativeImage img = tex.getPixels();
                if (img == null) return;
                for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setPixel(x, y, overlayArgb);
                tex.upload();
            } catch (Throwable ignored) {
            }
        }

        /** In the overlay shader alpha is the weight of the ORIGINAL color, so invert the intensity. */
        private int target() {
            int a = Math.round(255f * (1f - intensity.f() / 100f));
            return (a << 24) | (color.get() & 0xFFFFFF);
        }

        @Override
        public void onTick(Minecraft mc) {
            if (mc.gameRenderer == null) return;
            int t = target();
            if (color.chroma || t != applied) {
                apply(t);
                applied = t;
            }
        }

        @Override
        public void onDisable() {
            apply(0xB2FF0000);
            applied = 0;
        }
    }

    // ------------------------------------------------------------------ Block outline
    public static class BlockOutline extends Module {
        public static BlockOutline INSTANCE;
        public final Setting.Color color = add(new Setting.Color("color", "Outline Color", 0xFF000000, true, false));
        public final Setting.Num width = add(new Setting.Num("width", "Line Width", 1, 10, 0.5, 3, "px"));

        public BlockOutline() {
            super("block_outline", "Block Outline", "Custom color and thickness for the block you look at.", Items.GLASS, Category.VISUAL, false);
            INSTANCE = this;
        }

        public static boolean active() {
            return INSTANCE != null && INSTANCE.enabled;
        }
    }

    // ------------------------------------------------------------------ Nametag ping
    public static class NametagPing extends Module {
        public static NametagPing INSTANCE;
        public final Setting.Bool colorByPing = add(new Setting.Bool("color_by_ping", "Color By Ping", true));
        public final Setting.Bool self = add(new Setting.Bool("self", "Show On Yourself", false));

        public NametagPing() {
            super("nametag_ping", "Nametag Ping", "Shows the ping of players above their head.", Items.NAME_TAG, Category.VISUAL, false);
            INSTANCE = this;
        }

        public Component decorate(Component original, int ping) {
            int c = 0xAAAAAA;
            if (colorByPing.value) c = ping < 80 ? 0x55FF55 : ping < 160 ? 0xFFFF55 : ping < 300 ? 0xFFAA00 : 0xFF5555;
            final int col = c;
            return Component.empty().append(original).append(Component.literal(" " + ping + "ms").withStyle(s -> s.withColor(col)));
        }
    }

    // ------------------------------------------------------------------ Particles
    public static class Particles extends Module {
        public static Particles INSTANCE;
        private final Map<Identifier, Setting.Bool> map = new HashMap<>();

        public Particles() {
            super("particles", "Particles", "Turn single particle types on or off.", Items.FIREWORK_STAR, Category.VISUAL, false);
            INSTANCE = this;
            add(new Setting.Action("Show All Particles", () -> map.values().forEach(b -> b.value = true)));
            add(new Setting.Action("Hide All Particles", () -> map.values().forEach(b -> b.value = false)));
            TreeMap<String, Identifier> sorted = new TreeMap<>();
            for (Identifier id : BuiltInRegistries.PARTICLE_TYPE.keySet()) sorted.put(id.getPath(), id);
            for (var e : sorted.entrySet()) {
                Setting.Bool b = add(new Setting.Bool("p_" + e.getKey(), pretty(e.getKey()), true));
                map.put(e.getValue(), b);
            }
        }

        private static String pretty(String path) {
            StringBuilder sb = new StringBuilder();
            for (String part : path.split("_")) {
                if (part.isEmpty()) continue;
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            return sb.toString();
        }

        public static boolean hidden(ParticleOptions options) {
            Particles p = INSTANCE;
            if (p == null || !p.enabled) return false;
            try {
                ParticleType<?> type = options.getType();
                Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
                Setting.Bool b = p.map.get(id);
                return b != null && !b.value;
            } catch (Throwable t) {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------ F3 menu
    public static class F3Menu extends Module {
        private static final String[] OPTS = {"Default", "In F3", "Always", "Hidden"};
        private final List<Identifier> ids = new ArrayList<>();
        private final List<Setting.Choice> choices = new ArrayList<>();
        private final Map<Identifier, DebugScreenEntryStatus> original = new HashMap<>();
        private final Map<Identifier, Integer> applied = new HashMap<>();

        public F3Menu() {
            super("f3_menu", "F3 Menu", "Choose which lines the debug screen (F3) shows.", Items.SPYGLASS, Category.UTILITY, false);
            e(DebugScreenEntries.GAME_VERSION, "Game Version");
            e(DebugScreenEntries.FPS, "FPS");
            e(DebugScreenEntries.TPS, "TPS");
            e(DebugScreenEntries.MEMORY, "Memory");
            e(DebugScreenEntries.SYSTEM_SPECS, "System Specs");
            e(DebugScreenEntries.PLAYER_POSITION, "Position");
            e(DebugScreenEntries.PLAYER_SECTION_POSITION, "Chunk Position");
            e(DebugScreenEntries.BIOME, "Biome");
            e(DebugScreenEntries.LIGHT_LEVELS, "Light Levels");
            e(DebugScreenEntries.HEIGHTMAP, "Heightmap");
            e(DebugScreenEntries.LOCAL_DIFFICULTY, "Local Difficulty");
            e(DebugScreenEntries.LOOKING_AT_BLOCK, "Looking At Block");
            e(DebugScreenEntries.LOOKING_AT_FLUID, "Looking At Fluid");
            e(DebugScreenEntries.LOOKING_AT_ENTITY, "Looking At Entity");
            e(DebugScreenEntries.CHUNK_RENDER_STATS, "Chunk Render Stats");
            e(DebugScreenEntries.CHUNK_GENERATION_STATS, "Chunk Generation Stats");
            e(DebugScreenEntries.CHUNK_SOURCE_STATS, "Chunk Source Stats");
            e(DebugScreenEntries.ENTITY_RENDER_STATS, "Entity Render Stats");
            e(DebugScreenEntries.PARTICLE_RENDER_STATS, "Particle Stats");
            e(DebugScreenEntries.ENTITY_SPAWN_COUNTS, "Spawn Counts");
            e(DebugScreenEntries.SOUND_MOOD, "Sound Mood");
            e(DebugScreenEntries.POST_EFFECT, "Post Effect");
            e(DebugScreenEntries.GPU_UTILIZATION, "GPU Utilization");
            e(DebugScreenEntries.ENTITY_HITBOXES, "Entity Hitboxes");
            e(DebugScreenEntries.CHUNK_BORDERS, "Chunk Borders");
            e(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR, "3D Crosshair");
            e(DebugScreenEntries.CHUNK_SECTION_PATHS, "Chunk Section Paths");
        }

        private void e(Identifier id, String name) {
            ids.add(id);
            choices.add(add(new Setting.Choice("f3_" + id.getPath(), name, 0, OPTS)));
        }

        private void applyAll(Minecraft mc, boolean restore) {
            if (mc.debugEntries == null) return;
            for (int i = 0; i < ids.size(); i++) {
                Identifier id = ids.get(i);
                int idx = restore ? 0 : choices.get(i).index;
                Integer last = applied.get(id);
                if (last != null && last == idx) continue;
                if (!original.containsKey(id)) original.put(id, mc.debugEntries.getStatus(id));
                DebugScreenEntryStatus st = switch (idx) {
                    case 1 -> DebugScreenEntryStatus.IN_OVERLAY;
                    case 2 -> DebugScreenEntryStatus.ALWAYS_ON;
                    case 3 -> DebugScreenEntryStatus.NEVER;
                    default -> original.get(id);
                };
                if (st != null) mc.debugEntries.setStatus(id, st);
                applied.put(id, idx);
            }
        }

        @Override
        public void onTick(Minecraft mc) {
            applyAll(mc, false);
        }

        @Override
        public void onDisable() {
            applyAll(Minecraft.getInstance(), true);
            applied.clear();
        }
    }

    // ------------------------------------------------------------------ No fog
    public static class NoFog extends Module {
        public NoFog() {
            super("no_fog", "No Fog", "Removes the fog.", Items.COBWEB, Category.VISUAL, false);
        }

        @Override
        public void onTick(Minecraft mc) {
            try {
                FogRendererAccessor.vanillahud$setFogEnabled(false);
            } catch (Throwable ignored) {
            }
        }

        @Override
        public void onDisable() {
            try {
                FogRendererAccessor.vanillahud$setFogEnabled(true);
            } catch (Throwable ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ Weather
    public static class Weather extends Module {
        public final Setting.Choice mode = add(new Setting.Choice("mode", "Weather", 0, "Clear", "Rain", "Thunder"));

        public Weather() {
            super("weather", "Weather Changer", "Changes the weather you see (client side).", Items.WATER_BUCKET, Category.VISUAL, false);
        }

        @Override
        public void onTick(Minecraft mc) {
            if (mc.level == null) return;
            switch (mode.get()) {
                case "Clear" -> {
                    mc.level.setRainLevel(0f);
                    mc.level.setThunderLevel(0f);
                }
                case "Rain" -> {
                    mc.level.setRainLevel(1f);
                    mc.level.setThunderLevel(0f);
                }
                default -> {
                    mc.level.setRainLevel(1f);
                    mc.level.setThunderLevel(1f);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Crosshair
    public static class Crosshair extends Module {
        public final Setting.Choice style = add(new Setting.Choice("style", "Style", 0, "Cross", "Dot", "Circle", "Square"));
        public final Setting.Color color = add(new Setting.Color("color", "Color", 0xFFFFFFFF, true, false));
        public final Setting.Bool onTarget = add(new Setting.Bool("on_target", "Change Color On Target", true));
        public final Setting.Color targetColor = add(new Setting.Color("target_color", "Target Color", 0xFFFF3030, true, false));
        public final Setting.Num size = add(new Setting.Num("size", "Size", 1, 20, 1, 5, "px"));
        public final Setting.Num thickness = add(new Setting.Num("thickness", "Thickness", 1, 6, 1, 1, "px"));
        public final Setting.Num gap = add(new Setting.Num("gap", "Gap", 0, 12, 1, 2, "px"));
        public final Setting.Bool centerDot = add(new Setting.Bool("center_dot", "Center Dot", false));
        public final Setting.Bool outline = add(new Setting.Bool("outline", "Outline", true));
        public final Setting.Color outlineColor = add(new Setting.Color("outline_color", "Outline Color", 0xFF000000, true, false));

        public Crosshair() {
            super("crosshair", "Crosshair", "Custom crosshair style, color and size.", Items.TARGET, Category.VISUAL, false);
            targetColor.dependsOn(onTarget);
            outlineColor.dependsOn(outline);
        }

        public void register() {
            HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, orig -> (g, tracker) -> {
                Minecraft mc = Minecraft.getInstance();
                if (!enabled || mc.options.hideGui) {
                    orig.render(g, tracker);
                    return;
                }
                if (mc.screen != null) return;
                draw(g, mc);
            });
        }

        private void px(GuiGraphics g, int x, int y, int w, int h, int c, boolean outl) {
            if (outl) g.fill(x - 1, y - 1, x + w + 1, y + h + 1, outlineColor.get());
            else g.fill(x, y, x + w, y + h, c);
        }

        private void rect(GuiGraphics g, int x, int y, int w, int h, int c) {
            if (outline.value) g.fill(x - 1, y - 1, x + w + 1, y + h + 1, outlineColor.get());
            g.fill(x, y, x + w, y + h, c);
        }

        public void draw(GuiGraphics g, Minecraft mc) {
            int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
            int c = onTarget.value && mc.crosshairPickEntity != null ? targetColor.get() : color.get();
            int t = thickness.i(), s = size.i(), gp = gap.i();
            int o = t / 2;
            switch (style.get()) {
                case "Cross" -> {
                    // outlines first so they don't cover neighbouring arms
                    for (int pass = outline.value ? 0 : 1; pass < 2; pass++) {
                        int e = pass == 0 ? 1 : 0;
                        int col = pass == 0 ? outlineColor.get() : c;
                        g.fill(cx - o - e, cy - gp - s - e, cx - o + t + e, cy - gp + e, col);
                        g.fill(cx - o - e, cy + gp + 1 - e, cx - o + t + e, cy + gp + 1 + s + e, col);
                        g.fill(cx - gp - s - e, cy - o - e, cx - gp + e, cy - o + t + e, col);
                        g.fill(cx + gp + 1 - e, cy - o - e, cx + gp + 1 + s + e, cy - o + t + e, col);
                    }
                }
                case "Dot" -> rect(g, cx - s / 2, cy - s / 2, Math.max(1, s), Math.max(1, s), c);
                case "Square" -> {
                    int r = s + gp;
                    for (int pass = outline.value ? 0 : 1; pass < 2; pass++) {
                        int e = pass == 0 ? 1 : 0;
                        int col = pass == 0 ? outlineColor.get() : c;
                        g.fill(cx - r - e, cy - r - e, cx + r + 1 + e, cy - r + t + e, col);
                        g.fill(cx - r - e, cy + r + 1 - t - e, cx + r + 1 + e, cy + r + 1 + e, col);
                        g.fill(cx - r - e, cy - r - e, cx - r + t + e, cy + r + 1 + e, col);
                        g.fill(cx + r + 1 - t - e, cy - r - e, cx + r + 1 + e, cy + r + 1 + e, col);
                    }
                }
                default -> { // Circle
                    int r = s + gp;
                    int steps = Math.max(24, r * 8);
                    if (outline.value) {
                        for (int i = 0; i < steps; i++) {
                            double a = i * 2 * Math.PI / steps;
                            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
                            g.fill(x - 1, y - 1, x + t + 1, y + t + 1, outlineColor.get());
                        }
                    }
                    for (int i = 0; i < steps; i++) {
                        double a = i * 2 * Math.PI / steps;
                        int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
                        g.fill(x, y, x + t, y + t, c);
                    }
                }
            }
            if (centerDot.value && !style.is("Dot")) rect(g, cx - o, cy - o, t, t, c);
        }
    }

    // ------------------------------------------------------------------ Hide vanilla HUD parts
    public static class HudCleaner extends Module {
        private final List<Setting.Bool> hides = new ArrayList<>();

        public HudCleaner() {
            super("hud_cleaner", "HUD Cleaner", "Hide single parts of the vanilla HUD.", Items.SHEARS, Category.VISUAL, false);
            hook(VanillaHudElements.ARMOR_BAR, "Hide Armor Bar");
            hook(VanillaHudElements.HEALTH_BAR, "Hide Health Bar");
            hook(VanillaHudElements.FOOD_BAR, "Hide Hunger Bar");
            hook(VanillaHudElements.AIR_BAR, "Hide Air Bar");
            hook(VanillaHudElements.EXPERIENCE_LEVEL, "Hide XP Level");
            hook(VanillaHudElements.INFO_BAR, "Hide XP Bar");
            hook(VanillaHudElements.HELD_ITEM_TOOLTIP, "Hide Held Item Name");
            hook(VanillaHudElements.STATUS_EFFECTS, "Hide Potion Icons");
            hook(VanillaHudElements.SCOREBOARD, "Hide Scoreboard");
            hook(VanillaHudElements.BOSS_BAR, "Hide Boss Bars");
            hook(VanillaHudElements.TITLE_AND_SUBTITLE, "Hide Titles");
            hook(VanillaHudElements.OVERLAY_MESSAGE, "Hide Action Bar Text");
            hook(VanillaHudElements.SUBTITLES, "Hide Subtitles");
        }

        private void hook(Identifier id, String name) {
            Setting.Bool b = add(new Setting.Bool("hide_" + id.getPath(), name, false));
            hides.add(b);
            HudElementRegistry.replaceElement(id, orig -> (g, tracker) -> {
                if (!(enabled && b.value)) orig.render(g, tracker);
            });
        }
    }
}
