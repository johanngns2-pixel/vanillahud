package de.vanillahud.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Small drawing helpers that use the vanilla widget sprites. */
public final class Ui {
    private static final Identifier BTN = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BTN_HI = Identifier.withDefaultNamespace("widget/button_highlighted");
    private static final Identifier BTN_OFF = Identifier.withDefaultNamespace("widget/button_disabled");
    private static final Identifier SLIDER = Identifier.withDefaultNamespace("widget/slider");
    private static final Identifier SLIDER_HI = Identifier.withDefaultNamespace("widget/slider_highlighted");
    private static final Identifier HANDLE = Identifier.withDefaultNamespace("widget/slider_handle");
    private static final Identifier HANDLE_HI = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");

    private Ui() {}

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static void button(GuiGraphics g, Font font, int x, int y, int w, int h, Component label, boolean hover, boolean active) {
        Identifier id = !active ? BTN_OFF : hover ? BTN_HI : BTN;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, active ? 0xFFFFFFFF : 0xFFA0A0A0);
    }

    public static void slider(GuiGraphics g, Font font, int x, int y, int w, int h, Component label, double frac, boolean hover) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? SLIDER_HI : SLIDER, x, y, w, h);
        int hx = x + (int) (frac * (w - 8));
        g.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? HANDLE_HI : HANDLE, hx, y, 8, h);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    /** Dark card with a light top/left and dark bottom/right edge (inventory look). */
    public static void card(GuiGraphics g, int x, int y, int w, int h, boolean accent) {
        g.fill(x, y, x + w, y + h, 0xFF373737);
        g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
        if (accent) g.fill(x + 1, y + 1, x + 3, y + h - 1, 0xFF55FF55);
    }

    /** Small description box next to the mouse. */
    public static void tooltip(GuiGraphics g, Font font, String text, int mx, int my, int sw, int sh) {
        var lines = font.split(Component.literal(text), 190);
        if (lines.isEmpty()) return;
        int w = 0;
        for (var l : lines) w = Math.max(w, font.width(l));
        int h = lines.size() * 10;
        int x = Math.min(mx + 10, sw - w - 10);
        int y = Math.max(4, Math.min(my - 6, sh - h - 10));
        g.fill(x - 4, y - 4, x + w + 4, y + h + 2, 0xF0100010);
        g.renderOutline(x - 4, y - 4, w + 8, h + 6, 0xFF5000B0);
        int i = 0;
        for (var l : lines) g.drawString(font, l, x, y + 10 * i++, 0xFFFFFFFF, true);
    }

    public static void scrollbar(GuiGraphics g, int x, int top, int bottom, double scroll, double max, int contentH) {
        if (max <= 0) return;
        int h = bottom - top;
        int barH = Math.max(16, (int) (h * (double) h / contentH));
        int barY = top + (int) ((h - barH) * (scroll / max));
        g.fill(x, top, x + 4, bottom, 0x80000000);
        g.fill(x, barY, x + 4, barY + barH, 0xFFAAAAAA);
    }
}
