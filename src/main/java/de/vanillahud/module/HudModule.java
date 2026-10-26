package de.vanillahud.module;

import de.vanillahud.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;

/** A module that draws a box on the HUD. Position, scale and colors are configurable. */
public abstract class HudModule extends Module {
    public int x, y;

    public final Setting.Num scale = add(new Setting.Num("scale", "Scale", 0.5, 3.0, 0.1, 1.0, "x"));
    public final Setting.Num padding = add(new Setting.Num("padding", "Padding", 0, 12, 1, 4, "px"));
    public final Setting.Bool background = add(new Setting.Bool("bg", "Background", true));
    public final Setting.Color bgColor = add(new Setting.Color("bg_color", "Background Color", 0x90000000, true, false));
    public final Setting.Bool border = add(new Setting.Bool("border", "Border", false));
    public final Setting.Color borderColor = add(new Setting.Color("border_color", "Border Color", 0xFFFFFFFF, true, false));
    public final Setting.Color textColor = add(new Setting.Color("text_color", "Text Color", 0xFFFFFFFF));
    public final Setting.Bool shadow = add(new Setting.Bool("shadow", "Text Shadow", true));

    protected HudModule(String id, String name, String description, Item icon, Category category, boolean enabled) {
        super(id, name, description, icon, category, enabled);
        bgColor.dependsOn(background);
        borderColor.dependsOn(border);
    }

    @Override
    public boolean isHud() {
        return true;
    }

    protected abstract int contentWidth(Minecraft mc);

    protected abstract int contentHeight(Minecraft mc);

    /** Draw the content with its top-left at (cx, cy) (already translated and scaled). */
    protected abstract void drawContent(GuiGraphics g, Minecraft mc, int cx, int cy, int color);

    public int width(Minecraft mc) {
        return Math.max(4, Math.round((contentWidth(mc) + 2 * padding.i()) * scale.f()));
    }

    public int height(Minecraft mc) {
        return Math.max(4, Math.round((contentHeight(mc) + 2 * padding.i()) * scale.f()));
    }

    public boolean hit(Minecraft mc, double mx, double my) {
        return mx >= x && mx <= x + width(mc) && my >= y && my <= y + height(mc);
    }

    public void render(GuiGraphics g, Minecraft mc) {
        render(g, mc, x, y);
    }

    public void render(GuiGraphics g, Minecraft mc, int px, int py) {
        int pad = padding.i();
        int w = contentWidth(mc) + 2 * pad;
        int h = contentHeight(mc) + 2 * pad;
        g.pose().pushMatrix();
        g.pose().translate((float) px, (float) py);
        g.pose().scale(scale.f(), scale.f());
        if (background.value) g.fill(0, 0, w, h, bgColor.get());
        if (border.value) g.renderOutline(0, 0, w, h, borderColor.get());
        drawContent(g, mc, pad, pad, textColor.get());
        g.pose().popMatrix();
    }
}
