package de.vanillahud.module.impl;

import de.vanillahud.module.HudModule;
import de.vanillahud.setting.BoolSetting;
import de.vanillahud.setting.EnumSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Ruestung (und optional Hand-Item) mit Icon und Haltbarkeit. */
public class ArmorStatusModule extends HudModule {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    public final EnumSetting durability = add(new EnumSetting("durability", 0, "number", "percent", "off"));
    public final BoolSetting showHand = add(new BoolSetting("showHand", true));
    public final BoolSetting showOffhand = add(new BoolSetting("showOffhand", false));

    private record Row(ItemStack stack, String text) {}

    public ArmorStatusModule(int x, int y, boolean on) {
        super("armorstatus", Items.DIAMOND_CHESTPLATE, x, y, on, true);
    }

    private List<Row> rows(Minecraft mc) {
        List<Row> rows = new ArrayList<>();
        if (mc.player == null) return rows;
        for (int i = 0; i < SLOTS.length; i++) {
            if (i == 4 && !showHand.value) continue;
            if (i == 5 && !showOffhand.value) continue;
            ItemStack st = mc.player.getItemBySlot(SLOTS[i]);
            if (st.isEmpty()) continue;
            String t = "";
            if (st.isDamageableItem() && !durability.get().equals("off")) {
                int left = st.getMaxDamage() - st.getDamageValue();
                t = durability.get().equals("percent") ? (left * 100 / Math.max(1, st.getMaxDamage())) + "%"
                        : left + "/" + st.getMaxDamage();
            } else if (st.getCount() > 1) {
                t = "x" + st.getCount();
            }
            rows.add(new Row(st, t));
        }
        return rows;
    }

    @Override
    protected int contentW(Minecraft mc) {
        int w = 40;
        for (Row r : rows(mc)) w = Math.max(w, 18 + mc.font.width(r.text));
        return w;
    }

    @Override
    protected int contentH(Minecraft mc) {
        return Math.max(16, rows(mc).size() * 16);
    }

    @Override
    protected void drawContent(GuiGraphics g, Minecraft mc, int color) {
        int yy = PAD;
        for (Row r : rows(mc)) {
            g.renderItem(r.stack, PAD, yy);
            g.drawString(mc.font, r.text, PAD + 18, yy + 4, color, shadow.value);
            yy += 16;
        }
    }
}
