package de.vanillahud.screen;

import de.vanillahud.module.Modules;
import de.vanillahud.setting.ColorSetting;
import de.vanillahud.util.Colors;
import de.vanillahud.util.I18n;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Farbwaehler: Farbflaeche (Saettigung/Helligkeit), Farbton-Leiste, Schnellfarben und Hex-Feld. */
public class ColorPickerScreen extends Screen {
    private static final int SZ = 100;
    private static final int PRESET = 18;

    private final Screen parent;
    private final ColorSetting setting;
    private final String label;
    private final int original;
    private float h, s, v;
    private int ox, oy, hx, py;
    private boolean dragSV, dragH, updating;
    private EditBox hex;

    public ColorPickerScreen(Screen parent, ColorSetting setting, String label) {
        super(Component.literal(label));
        this.parent = parent;
        this.setting = setting;
        this.label = label;
        this.original = setting.value;
        float[] hsv = Colors.rgbToHsv(setting.value);
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];
    }

    @Override
    protected void init() {
        ox = width / 2 - 80;
        oy = 40;
        hx = ox + SZ + 12;
        py = oy + SZ + 38;

        hex = new EditBox(font, width / 2 - 45, oy + SZ + 10, 90, 18, Component.literal("hex"));
        hex.setMaxLength(6);
        hex.setFilter(str -> str.matches("[0-9a-fA-F]*"));
        hex.setValue(setting.hex());
        hex.setResponder(str -> {
            if (!updating && str.length() == 6) {
                try {
                    setting.value = Integer.parseInt(str, 16) & 0xFFFFFF;
                    float[] hsv = Colors.rgbToHsv(setting.value);
                    h = hsv[0];
                    s = hsv[1];
                    v = hsv[2];
                } catch (NumberFormatException ignored) {
                }
            }
        });
        addRenderableWidget(hex);

        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.done")), b -> onClose())
                .bounds(width / 2 - 102, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.cancel")), b -> {
            setting.value = original;
            onClose();
        }).bounds(width / 2 + 2, height - 28, 100, 20).build());
    }

    private void apply() {
        setting.value = Colors.hsvToRgb(h, s, v) & 0xFFFFFF;
        updating = true;
        hex.setValue(setting.hex());
        updating = false;
    }

    private void pick(double mx, double my) {
        if (dragSV) {
            s = (float) Math.max(0, Math.min(1, (mx - ox) / (SZ - 1)));
            v = 1f - (float) Math.max(0, Math.min(1, (my - oy) / (SZ - 1)));
            apply();
        } else if (dragH) {
            h = (float) Math.max(0, Math.min(1, (my - oy) / (SZ - 1)));
            apply();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (super.mouseClicked(e, dbl)) return true;
        double mx = e.x(), my = e.y();
        if (mx >= ox && mx < ox + SZ && my >= oy && my < oy + SZ) {
            dragSV = true;
            pick(mx, my);
            return true;
        }
        if (mx >= hx && mx < hx + 14 && my >= oy && my < oy + SZ) {
            dragH = true;
            pick(mx, my);
            return true;
        }
        int perRow = 8;
        int pLeft = width / 2 - perRow * PRESET / 2;
        for (int i = 0; i < Colors.PRESETS.length; i++) {
            int px = pLeft + (i % perRow) * PRESET, pyy = py + (i / perRow) * PRESET;
            if (mx >= px && mx < px + PRESET - 2 && my >= pyy && my < pyy + PRESET - 2) {
                setting.value = Colors.PRESETS[i];
                float[] hsv = Colors.rgbToHsv(setting.value);
                h = hsv[0];
                s = hsv[1];
                v = hsv[2];
                updating = true;
                hex.setValue(setting.hex());
                updating = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragSV || dragH) {
            pick(e.x(), e.y());
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        dragSV = false;
        dragH = false;
        return super.mouseReleased(e);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, label, width / 2, 16, 0xFFFFFFFF);

        // Farbflaeche: links Weiss -> rechts Vollfarbe, nach unten Schwarz
        for (int x = 0; x < SZ; x++) {
            int top = 0xFF000000 | Colors.hsvToRgb(h, x / (float) (SZ - 1), 1f);
            g.fillGradient(ox + x, oy, ox + x + 1, oy + SZ, top, 0xFF000000);
        }
        g.renderOutline(ox - 1, oy - 1, SZ + 2, SZ + 2, 0xFFFFFFFF);
        int mx = ox + Math.round(s * (SZ - 1)), my = oy + Math.round((1f - v) * (SZ - 1));
        g.renderOutline(mx - 3, my - 3, 7, 7, 0xFFFFFFFF);
        g.renderOutline(mx - 2, my - 2, 5, 5, 0xFF000000);

        // Farbton-Leiste
        for (int y = 0; y < SZ; y++) {
            g.fill(hx, oy + y, hx + 14, oy + y + 1, 0xFF000000 | Colors.hsvToRgb(y / (float) (SZ - 1), 1f, 1f));
        }
        g.renderOutline(hx - 1, oy - 1, 16, SZ + 2, 0xFFFFFFFF);
        int hy = oy + Math.round(h * (SZ - 1));
        g.fill(hx - 3, hy - 1, hx + 17, hy + 2, 0xFFFFFFFF);
        g.fill(hx - 2, hy, hx + 16, hy + 1, 0xFF000000);

        // alt / neu
        int px = hx + 28;
        g.drawString(font, I18n.tr("ui.old"), px, oy, 0xFFAAAAAA, true);
        g.fill(px, oy + 12, px + 34, oy + 38, 0xFF000000 | original);
        g.drawString(font, I18n.tr("ui.new"), px, oy + 46, 0xFFAAAAAA, true);
        g.fill(px, oy + 58, px + 34, oy + 84, 0xFF000000 | setting.value);
        g.renderOutline(px - 1, oy + 11, 36, 28, 0xFFFFFFFF);
        g.renderOutline(px - 1, oy + 57, 36, 28, 0xFFFFFFFF);

        // Schnellfarben
        int perRow = 8;
        int pLeft = width / 2 - perRow * PRESET / 2;
        for (int i = 0; i < Colors.PRESETS.length; i++) {
            int x = pLeft + (i % perRow) * PRESET, y = py + (i / perRow) * PRESET;
            g.fill(x - 1, y - 1, x + PRESET - 1, y + PRESET - 1, 0xFFFFFFFF);
            g.fill(x, y, x + PRESET - 2, y + PRESET - 2, 0xFF000000 | Colors.PRESETS[i]);
        }
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parent);
    }
}
