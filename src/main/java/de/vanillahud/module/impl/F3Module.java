package de.vanillahud.module.impl;

import de.vanillahud.module.HudModule;
import de.vanillahud.module.Modules;
import de.vanillahud.setting.BoolSetting;
import de.vanillahud.util.I18n;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Eigenes, frei zusammenstellbares F3-Menue: jede Zeile einzeln an/aus. */
public class F3Module extends HudModule {
    private final BoolSetting version = add(new BoolSetting("f3_version", false));
    private final BoolSetting fps = add(new BoolSetting("f3_fps", true));
    private final BoolSetting xyz = add(new BoolSetting("f3_xyz", true));
    private final BoolSetting block = add(new BoolSetting("f3_block", false));
    private final BoolSetting chunk = add(new BoolSetting("f3_chunk", false));
    private final BoolSetting facing = add(new BoolSetting("f3_facing", true));
    private final BoolSetting biome = add(new BoolSetting("f3_biome", true));
    private final BoolSetting light = add(new BoolSetting("f3_light", false));
    private final BoolSetting target = add(new BoolSetting("f3_target", false));
    private final BoolSetting ping = add(new BoolSetting("f3_ping", false));
    private final BoolSetting tps = add(new BoolSetting("f3_tps", false));
    private final BoolSetting memory = add(new BoolSetting("f3_memory", false));
    private final BoolSetting entities = add(new BoolSetting("f3_entities", false));
    private final BoolSetting worldtime = add(new BoolSetting("f3_worldtime", false));

    public F3Module(int x, int y, boolean on) {
        super("f3", Items.WRITABLE_BOOK, x, y, on, true);
    }

    private List<String> lines(Minecraft mc) {
        List<String> l = new ArrayList<>();
        Player p = mc.player;
        if (version.value) {
            String v = FabricLoader.getInstance().getModContainer("minecraft")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
            l.add("Minecraft " + v);
        }
        if (fps.value) l.add(mc.getFps() + " fps");
        if (p == null || mc.level == null) return l;
        BlockPos bp = p.blockPosition();
        if (xyz.value) l.add(String.format("XYZ: %.3f / %.5f / %.3f", p.getX(), p.getY(), p.getZ()));
        if (block.value) l.add(I18n.tr("f3.block", bp.getX(), bp.getY(), bp.getZ()));
        if (chunk.value) l.add(I18n.tr("f3.chunk", bp.getX() >> 4, bp.getZ() >> 4));
        if (facing.value) {
            l.add(I18n.tr("f3.facing", p.getDirection().getName(), Mth.wrapDegrees(p.getYRot()), Mth.wrapDegrees(p.getXRot())));
        }
        if (biome.value) l.add(I18n.tr("f3.biome", mc.level.getBiome(bp).getRegisteredName().replace("minecraft:", "")));
        if (light.value) l.add(I18n.tr("f3.light", mc.level.getMaxLocalRawBrightness(bp)));
        if (target.value) {
            String t = Modules.targetName();
            if (!t.isEmpty()) l.add(I18n.tr("f3.target", t));
        }
        if (ping.value) l.add(I18n.tr("f3.ping", Modules.ping()));
        if (tps.value) l.add(I18n.tr("f3.tps", String.format("%.1f", Modules.tps())));
        if (memory.value) {
            Runtime r = Runtime.getRuntime();
            l.add(I18n.tr("hud.ram", (r.totalMemory() - r.freeMemory()) / 1048576L, r.maxMemory() / 1048576L));
        }
        if (entities.value) l.add(I18n.tr("f3.entities", mc.level.getEntityCount()));
        if (worldtime.value) l.add(I18n.tr("f3.worldtime", Modules.worldTime()));
        return l;
    }

    @Override
    protected int contentW(Minecraft mc) {
        int w = 60;
        for (String s : lines(mc)) w = Math.max(w, mc.font.width(s));
        return w;
    }

    @Override
    protected int contentH(Minecraft mc) {
        return Math.max(1, lines(mc).size()) * 10 - 1;
    }

    @Override
    protected void drawContent(GuiGraphics g, Minecraft mc, int color) {
        int yy = PAD;
        for (String s : lines(mc)) {
            g.drawString(mc.font, s, PAD, yy, color, shadow.value);
            yy += 10;
        }
    }
}
