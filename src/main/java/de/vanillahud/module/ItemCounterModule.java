package de.vanillahud.module;

import de.vanillahud.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/** Counts items of given types in the inventory. */
public class ItemCounterModule extends HudModule {
    private final Set<Item> items;
    private final Item shown;
    public final Setting.Bool icon = add(new Setting.Bool("icon", "Show Icon", true));
    public final Setting.Bool label = add(new Setting.Bool("label", "Show Name", false));

    public ItemCounterModule(String id, String name, Item shown, Item... items) {
        super(id, name, "Counts " + name.toLowerCase() + " in your inventory.", shown, Category.PVP, false);
        this.shown = shown;
        this.items = Set.of(items);
    }

    private int count(Minecraft mc) {
        if (mc.player == null) return 0;
        Inventory inv = mc.player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && items.contains(s.getItem())) n += s.getCount();
        }
        return n;
    }

    private String text(Minecraft mc) {
        return label.value ? name + ": " + count(mc) : Integer.toString(count(mc));
    }

    @Override
    protected int contentWidth(Minecraft mc) {
        return (icon.value ? 19 : 0) + mc.font.width(text(mc));
    }

    @Override
    protected int contentHeight(Minecraft mc) {
        return icon.value ? 16 : 9;
    }

    @Override
    protected void drawContent(GuiGraphics g, Minecraft mc, int cx, int cy, int color) {
        int x = cx;
        if (icon.value) {
            g.renderItem(new ItemStack(shown), cx, cy);
            x += 19;
        }
        g.drawString(mc.font, text(mc), x, cy + (icon.value ? 4 : 0), color, shadow.value);
    }
}
