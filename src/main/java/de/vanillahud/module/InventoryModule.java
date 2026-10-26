package de.vanillahud.module;

import de.vanillahud.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InventoryModule extends HudModule {
    public final Setting.Bool hotbar = add(new Setting.Bool("hotbar", "Include Hotbar", false));

    public InventoryModule() {
        super("inventory", "Inventory Display", "Shows your inventory on the HUD.", Items.CHEST, Category.INFO, false);
    }

    private int rows() {
        return hotbar.value ? 4 : 3;
    }

    @Override
    protected int contentWidth(Minecraft mc) {
        return 9 * 18;
    }

    @Override
    protected int contentHeight(Minecraft mc) {
        return rows() * 18;
    }

    @Override
    protected void drawContent(GuiGraphics g, Minecraft mc, int cx, int cy, int color) {
        if (mc.player == null) return;
        var inv = mc.player.getInventory();
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < 9; c++) {
                int slot = hotbar.value && r == 3 ? c : 9 + (hotbar.value ? r : r) * 9 + c;
                if (hotbar.value && r < 3) slot = 9 + r * 9 + c;
                ItemStack st = inv.getItem(slot);
                if (st.isEmpty()) continue;
                g.renderItem(st, cx + c * 18 + 1, cy + r * 18 + 1);
                g.renderItemDecorations(mc.font, st, cx + c * 18 + 1, cy + r * 18 + 1);
            }
        }
    }
}
