package de.vanillahud.module;

import de.vanillahud.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorModule extends HudModule {
    public final Setting.Choice layout = add(new Setting.Choice("layout", "Layout", 0, "Vertical", "Horizontal"));
    public final Setting.Choice durability = add(new Setting.Choice("durability", "Durability", 0, "Number", "Percent", "Bar", "None"));
    public final Setting.Bool colorDur = add(new Setting.Bool("color_dur", "Color By Durability", true));
    public final Setting.Bool held = add(new Setting.Bool("held", "Show Held Item", true));

    public ArmorModule() {
        super("armor_status", "Armor Status", "Shows your armor and held item with durability.", Items.DIAMOND_CHESTPLATE, Category.PVP, false);
    }

    private List<ItemStack> stacks(Minecraft mc) {
        List<ItemStack> l = new ArrayList<>();
        if (mc.player == null) return l;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot s : slots) {
            ItemStack st = mc.player.getItemBySlot(s);
            if (!st.isEmpty()) l.add(st);
        }
        if (held.value) {
            ItemStack st = mc.player.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!st.isEmpty()) l.add(st);
        }
        return l;
    }

    private String text(ItemStack st) {
        if (!st.isDamageableItem()) return st.getCount() > 1 ? "x" + st.getCount() : "";
        int left = st.getMaxDamage() - st.getDamageValue();
        return switch (durability.get()) {
            case "Number" -> Integer.toString(left);
            case "Percent" -> (int) Math.round(left * 100.0 / st.getMaxDamage()) + "%";
            default -> "";
        };
    }

    private int durColor(ItemStack st, int def) {
        if (!colorDur.value || !st.isDamageableItem()) return def;
        float f = 1f - (float) st.getDamageValue() / st.getMaxDamage();
        return 0xFF000000 | de.vanillahud.util.Colors.hsv(f * 0.33f, 0.85f, 1f);
    }

    private boolean vertical() {
        return layout.is("Vertical");
    }

    @Override
    protected int contentWidth(Minecraft mc) {
        List<ItemStack> l = stacks(mc);
        if (l.isEmpty()) return 18;
        if (vertical()) {
            int w = 0;
            for (ItemStack s : l) w = Math.max(w, mc.font.width(text(s)));
            return 18 + (w > 0 ? 3 + w : 0);
        }
        int w = 0;
        for (ItemStack s : l) w += Math.max(18, mc.font.width(text(s))) + 2;
        return w - 2;
    }

    @Override
    protected int contentHeight(Minecraft mc) {
        List<ItemStack> l = stacks(mc);
        if (l.isEmpty()) return 18;
        if (vertical()) return l.size() * 18 - 2;
        return durability.is("Number") || durability.is("Percent") ? 18 + 10 : 18;
    }

    @Override
    protected void drawContent(GuiGraphics g, Minecraft mc, int cx, int cy, int color) {
        List<ItemStack> l = stacks(mc);
        int i = 0, hx = cx;
        for (ItemStack st : l) {
            String t = text(st);
            int c = durColor(st, color);
            if (vertical()) {
                int y = cy + i * 18;
                g.renderItem(st, cx, y);
                if (durability.is("Bar")) g.renderItemDecorations(mc.font, st, cx, y);
                if (!t.isEmpty()) g.drawString(mc.font, t, cx + 21, y + 5, c, shadow.value);
            } else {
                int w = Math.max(18, mc.font.width(t));
                g.renderItem(st, hx + (w - 16) / 2, cy);
                if (durability.is("Bar")) g.renderItemDecorations(mc.font, st, hx + (w - 16) / 2, cy);
                if (!t.isEmpty()) g.drawString(mc.font, t, hx + (w - mc.font.width(t)) / 2, cy + 18, c, shadow.value);
                hx += w + 2;
            }
            i++;
        }
    }
}
