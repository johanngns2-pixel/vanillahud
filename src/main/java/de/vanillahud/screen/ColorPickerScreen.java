package de.vanillahud.screen;

import de.vanillahud.module.Modules;
import de.vanillahud.setting.Setting;
import de.vanillahud.util.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Popup color picker: saturation/value square, hue bar, alpha bar, hex field, palette. */
public class ColorPickerScreen extends VhScreen {
    private static final int SZ = 120;
    private static final int[] PALETTE = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private final Screen parentScreen;
    private final Setting.Color setting;
    private float h, s, v;
    private int alpha;
    private int mode; // 1 = sv, 2 = hue, 3 = alpha
    private EditBox hex;
    private ThemedButton chromaBtn;
    private int px, py;

    public ColorPickerScreen(Screen parent, Setting.Color setting) {
        super(Component.literal(setting.name));
        this.parentScreen = parent;
        this.setting = setting;
        float[] hsv = Colors.toHsv(setting.argb & 0xFFFFFF);
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];
        alpha = setting.alpha ? (setting.argb >>> 24) : 255;
    }

    @Override
    protected void init() {
        px = width / 2 - 125;
        py = Math.max(4, height / 2 - 130);
        int rx = px + SZ + 34;

        hex = new EditBox(font, rx, py + 52, 92, 18, Component.literal("Hex"));
        hex.setMaxLength(9);
        hex.setValue(Colors.hex(setting.argb, setting.alpha));
        hex.setResponder(str -> {
            if (!hex.isFocused()) return;
            Integer c = Colors.parse(str, setting.alpha, alpha);
            if (c == null) return;
            float[] hsv = Colors.toHsv(c & 0xFFFFFF);
            h = hsv[0];
            s = hsv[1];
            v = hsv[2];
            if (setting.alpha && str.replace("#", "").length() == 8) alpha = c >>> 24;
            commit(false);
        });
        addRenderableWidget(hex);

        chromaBtn = ThemedButton.builder(chromaLabel(), b -> {
            setting.chroma = !setting.chroma;
            b.setMessage(chromaLabel());
            Modules.save();
        }).bounds(rx, py + 76, 92, 18).build();
        addRenderableWidget(chromaBtn);

        addRenderableWidget(ThemedButton.builder(CommonComponents.GUI_DONE, b -> onClose()).primary()
                .bounds(width / 2 - 50, py + SZ + 100, 100, 20).build());
    }

    private Component chromaLabel() {
        return Component.literal("Chroma: ").append(Ui.onOff(setting.chroma));
    }

    private void commit(boolean updateHex) {
        int rgb = Colors.hsv(h, s, v);
        setting.argb = ((setting.alpha ? alpha : 255) << 24) | rgb;
        if (updateHex) hex.setValue(Colors.hex(setting.argb, setting.alpha));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, title, width / 2, py - 2 < 12 ? 4 : py - 12, 0xFFFFFFFF);

        // saturation / value square
        int sy = py + 10;
        for (int i = 0; i < SZ; i++) {
            int top = 0xFF000000 | Colors.hsv(h, i / (float) (SZ - 1), 1f);
            g.fillGradient(px + i, sy, px + i + 1, sy + SZ, top, 0xFF000000);
        }
        int mxp = px + (int) (s * (SZ - 1)), myp = sy + (int) ((1 - v) * (SZ - 1));
        g.renderOutline(mxp - 3, myp - 3, 7, 7, 0xFF000000);
        g.renderOutline(mxp - 2, myp - 2, 5, 5, 0xFFFFFFFF);

        // hue bar
        int hx = px + SZ + 8;
        int seg = SZ / 6;
        for (int i = 0; i < 6; i++) {
            int c1 = 0xFF000000 | Colors.hsv(i / 6f, 1f, 1f);
            int c2 = 0xFF000000 | Colors.hsv((i + 1) / 6f, 1f, 1f);
            g.fillGradient(hx, sy + i * seg, hx + 16, sy + (i + 1) * seg, c1, c2);
        }
        int hy = sy + (int) (h * (SZ - 1));
        g.fill(hx - 2, hy - 1, hx + 18, hy + 2, 0xFFFFFFFF);
        g.fill(hx - 1, hy, hx + 17, hy + 1, 0xFF000000);

        // alpha bar
        int ay = sy + SZ + 8;
        if (setting.alpha) {
            g.fill(px, ay, px + SZ, ay + 10, 0xFF808080);
            for (int i = 0; i < SZ; i++) {
                int a = (int) (i / (float) (SZ - 1) * 255);
                g.fill(px + i, ay, px + i + 1, ay + 10, (a << 24) | Colors.hsv(h, s, v));
            }
            int ax = px + (int) (alpha / 255f * (SZ - 1));
            g.fill(ax - 1, ay - 2, ax + 2, ay + 12, 0xFFFFFFFF);
            g.drawString(font, "Opacity " + (alpha * 100 / 255) + "%", px, ay + 14, 0xFFAAAAAA, false);
        }

        // preview
        int rx = px + SZ + 34;
        g.fill(rx - 1, py + 9, rx + 93, py + 43, 0xFF000000);
        g.fill(rx, py + 10, rx + 92, py + 42, 0xFF808080);
        g.fill(rx, py + 10, rx + 92, py + 42, setting.get());

        // palette
        int palY = sy + SZ + (setting.alpha ? 34 : 14);
        for (int i = 0; i < PALETTE.length; i++) {
            int x = px + (i % 8) * 16, y = palY + (i / 8) * 16;
            g.fill(x, y, x + 14, y + 14, 0xFF000000);
            g.fill(x + 1, y + 1, x + 13, y + 13, 0xFF000000 | PALETTE[i]);
        }
        g.drawString(font, "Presets", rx, palY + 2, 0xFFAAAAAA, false);
    }

    private void update(double mx, double my) {
        int sy = py + 10;
        if (mode == 1) {
            s = (float) Math.max(0, Math.min(1, (mx - px) / (SZ - 1)));
            v = 1f - (float) Math.max(0, Math.min(1, (my - sy) / (SZ - 1)));
        } else if (mode == 2) {
            h = (float) Math.max(0, Math.min(0.999, (my - sy) / (SZ - 1)));
        } else if (mode == 3) {
            alpha = (int) Math.round(Math.max(0, Math.min(1, (mx - px) / (SZ - 1))) * 255);
        }
        commit(true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int sy = py + 10, hx = px + SZ + 8, ay = sy + SZ + 8;
        if (event.button() == 0) {
            if (Ui.in(mx, my, px, sy, SZ, SZ)) { mode = 1; update(mx, my); return true; }
            if (Ui.in(mx, my, hx, sy, 16, SZ)) { mode = 2; update(mx, my); return true; }
            if (setting.alpha && Ui.in(mx, my, px, ay, SZ, 10)) { mode = 3; update(mx, my); return true; }
            int palY = sy + SZ + (setting.alpha ? 34 : 14);
            for (int i = 0; i < PALETTE.length; i++) {
                int x = px + (i % 8) * 16, y = palY + (i / 8) * 16;
                if (Ui.in(mx, my, x, y, 14, 14)) {
                    float[] hsv = Colors.toHsv(PALETTE[i]);
                    h = hsv[0];
                    s = hsv[1];
                    v = hsv[2];
                    commit(true);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (mode != 0) {
            update(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (mode != 0) {
            mode = 0;
            Modules.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parentScreen);
    }
}
