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

    public static Component onOff(boolean on) {
        int c = (on ? Theme.I.onText : Theme.I.offText).get() & 0xFFFFFF;
        return Component.literal(on ? "ON" : "OFF").withStyle(st -> st.withColor(c));
    }

    public static void button(GuiGraphics g, Font font, int x, int y, int w, int h, Component label, boolean hover, boolean active) {
        button(g, font, x, y, w, h, label, hover, active, 0);
    }

    /** bgFlat: optional ARGB override of the flat background (0 = none). */
    public static void button(GuiGraphics g, Font font, int x, int y, int w, int h, Component label, boolean hover, boolean active, int bgFlat) {
        if (Theme.flat()) {
            Theme t = Theme.I;
            int bg = !active ? t.btnOff.get() : hover ? t.btnHover.get() : bgFlat != 0 ? bgFlat : t.btn.get();
            g.fill(x, y, x + w, y + h, t.btnBorder.get());
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
            g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, active ? t.btnText.get() : t.btnTextOff.get());
            return;
        }
        Identifier id = !active ? BTN_OFF : hover ? BTN_HI : BTN;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, active ? 0xFFFFFFFF : 0xFFA0A0A0);
    }

    public static void slider(GuiGraphics g, Font font, int x, int y, int w, int h, Component label, double frac, boolean hover) {
        int hx = x + (int) (frac * (w - 8));
        if (Theme.flat()) {
            Theme t = Theme.I;
            g.fill(x, y, x + w, y + h, t.btnBorder.get());
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, t.btnOff.get());
            g.fill(x + 1, y + 1, hx + 4, y + h - 1, t.sliderFill.get());
            g.fill(hx, y, hx + 8, y + h, t.sliderHandle.get());
            g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, t.btnText.get());
            return;
        }
        g.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? SLIDER_HI : SLIDER, x, y, w, h);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? HANDLE_HI : HANDLE, hx, y, 8, h);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    /** Dark card with a light top/left and dark bottom/right edge (inventory look). */
    public static void card(GuiGraphics g, int x, int y, int w, int h, boolean accent) {
        Theme t = Theme.I;
        g.fill(x, y, x + w, y + h, t.cardBg.get());
        g.fill(x, y, x + w, y + 1, t.cardLight.get());
        g.fill(x, y, x + 1, y + h, t.cardLight.get());
        g.fill(x, y + h - 1, x + w, y + h, t.cardDark.get());
        g.fill(x + w - 1, y, x + w, y + h, t.cardDark.get());
        if (accent) g.fill(x + 1, y + 1, x + 3, y + h - 1, t.accent.get());
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
        g.fill(x - 4, y - 4, x + w + 4, y + h + 2, Theme.I.tipBg.get());
        g.renderOutline(x - 4, y - 4, w + 8, h + 6, Theme.I.tipBorder.get());
        int i = 0;
        for (var l : lines) g.drawString(font, l, x, y + 10 * i++, Theme.I.tipText.get(), true);
    }

    public static void scrollbar(GuiGraphics g, int x, int top, int bottom, double scroll, double max, int contentH) {
        if (max <= 0) return;
        int h = bottom - top;
        int barH = Math.max(16, (int) (h * (double) h / contentH));
        int barY = top + (int) ((h - barH) * (scroll / max));
        g.fill(x, top, x + 4, bottom, 0x80000000);
        g.fill(x, barY, x + 4, barY + barH, Theme.I.scroll.get());
    }
}
