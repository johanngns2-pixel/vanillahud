package de.vanillahud.screen;

import de.vanillahud.module.HudModule;
import de.vanillahud.module.Modules;
import de.vanillahud.setting.*;
import de.vanillahud.util.I18n;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Einstellungen eines Moduls: Schalter, Slider, Hex-Farbfelder, Auswahlknoepfe. */
public class ModuleSettingsScreen extends Screen {
    private static final int ROW_H = 34;

    private final Screen parent;
    private final HudModule module;
    private final List<Label> labels = new ArrayList<>();
    private final List<Swatch> swatches = new ArrayList<>();

    private record Label(String text, int x, int y) {}
    private record Swatch(ColorSetting s, int x, int y) {}

    public ModuleSettingsScreen(Screen parent, HudModule module) {
        super(Component.literal(module.name()));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        labels.clear();
        swatches.clear();
        List<Setting> shown = new ArrayList<>();
        for (Setting st : module.settings) {
            if (st instanceof BoolSetting || st instanceof EnumSetting || st instanceof IntSetting || st instanceof ColorSetting) shown.add(st);
        }
        int n = shown.size();
        int avail = height - 36 - 34;
        int cols = 2;
        while (cols < 4 && ((n + cols - 1) / cols) * ROW_H > avail) cols++;
        int cw = Math.min(160, (width - 24) / cols - 8);
        int total = cols * cw + (cols - 1) * 8;
        int left = (width - total) / 2;

        for (int i = 0; i < n; i++) {
            Setting s = shown.get(i);
            int cx = left + (i % cols) * (cw + 8);
            int cy = 36 + (i / cols) * ROW_H;
            labels.add(new Label(I18n.tr("setting." + s.id), cx, cy));
            int wy = cy + 11;
            if (s instanceof BoolSetting b) {
                addRenderableWidget(Button.builder(boolText(b), btn -> {
                    b.value = !b.value;
                    btn.setMessage(boolText(b));
                }).bounds(cx, wy, cw, 20).build());
            } else if (s instanceof EnumSetting e) {
                addRenderableWidget(Button.builder(enumText(e), btn -> {
                    e.next();
                    btn.setMessage(enumText(e));
                }).bounds(cx, wy, cw, 20).build());
            } else if (s instanceof IntSetting is) {
                addRenderableWidget(new SettingSlider(cx, wy, cw, is));
            } else if (s instanceof ColorSetting c) {
                EditBox box = new EditBox(font, cx, wy, cw - 24, 20, Component.literal(s.id));
                box.setMaxLength(6);
                box.setFilter(str -> str.matches("[0-9a-fA-F]*"));
                box.setValue(c.hex());
                box.setResponder(str -> {
                    if (str.length() == 6) {
                        try {
                            c.value = Integer.parseInt(str, 16) & 0xFFFFFF;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                });
                addRenderableWidget(box);
                String lbl = I18n.tr("setting." + s.id);
                // Klick auf das Farbfeld oeffnet den Farbwaehler
                addRenderableWidget(Button.builder(Component.empty(),
                        btn -> minecraft.setScreen(new ColorPickerScreen(this, c, lbl)))
                        .bounds(cx + cw - 20, wy, 20, 20).build());
                swatches.add(new Swatch(c, cx + cw - 20, wy));
            }
        }

        int by = height - 28;
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.reset")), b -> {
            for (Setting s : module.settings) s.reset();
            Modules.save();
            minecraft.setScreen(new ModuleSettingsScreen(parent, module));
        }).bounds(width / 2 - 154, by, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.back")), b -> onClose())
                .bounds(width / 2 + 4, by, 150, 20).build());
    }

    private static Component boolText(BoolSetting b) {
        return Component.literal(I18n.tr(b.value ? "ui.on" : "ui.off"));
    }

    private static Component enumText(EnumSetting e) {
        return Component.literal(I18n.tr("opt." + e.get()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, module.name() + " - " + I18n.tr("ui.settings"), width / 2, 14, 0xFFFFFFFF);
        for (Label l : labels) g.drawString(font, l.text, l.x, l.y, 0xFFCCCCCC, true);
        for (Swatch s : swatches) {
            g.fill(s.x - 1, s.y - 1, s.x + 21, s.y + 21, 0xFFFFFFFF);
            g.fill(s.x, s.y, s.x + 20, s.y + 20, 0xFF000000 | s.s.value);
        }
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parent);
    }

    private static class SettingSlider extends AbstractSliderButton {
        private final IntSetting s;

        SettingSlider(int x, int y, int w, IntSetting s) {
            super(x, y, w, 20, Component.empty(), (s.value - s.min) / (double) (s.max - s.min));
            this.s = s;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (s == null) return; // wird vom Super-Konstruktor schon aufgerufen
            setMessage(Component.literal(s.value + s.unit));
        }

        @Override
        protected void applyValue() {
            s.set(s.min + (int) Math.round(this.value * (s.max - s.min)));
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            super.onRelease(event);
            Modules.save();
        }
    }
}
