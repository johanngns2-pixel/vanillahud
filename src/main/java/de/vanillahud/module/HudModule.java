package de.vanillahud.module;

import de.vanillahud.setting.*;
import de.vanillahud.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Ein Modul. Meistens ein verschiebbares HUD-Element (hud = true), kann aber auch ein reines
 * Werkzeug sein (Zoom, Fullbright, Hitcolour ...), das nur Einstellungen hat.
 * Jedes Modul bringt dieselben Grund-Einstellungen mit (Groesse, Farbe, Hintergrund ...).
 */
public class HudModule {
    public static final int PAD = 4;
    /** true solange der HUD-Editor offen ist: leere Module zeigen dann ihren Namen als Platzhalter. */
    public static boolean editing;

    public final String id;
    public final Item icon;
    public final boolean hud;
    public boolean enabled;
    public int x, y;

    public final List<Setting> settings = new ArrayList<>();
    public final IntSetting scale = add(new IntSetting("scale", 50, 300, 100, "%"));
    public final ColorSetting color = add(new ColorSetting("textColor", 0xFFFFFF));
    public final BoolSetting rainbow = add(new BoolSetting("rainbow", false));
    public final BoolSetting shadow = add(new BoolSetting("shadow", true));
    public final BoolSetting bg = add(new BoolSetting("bg", true));
    public final ColorSetting bgColor = add(new ColorSetting("bgColor", 0x000000));
    public final IntSetting bgAlpha = add(new IntSetting("bgAlpha", 0, 100, 56, "%"));
    public final BoolSetting border = add(new BoolSetting("border", false));
    public final ColorSetting borderColor = add(new ColorSetting("borderColor", 0xFFFFFF));

    public Supplier<String> text;
    public Consumer<Minecraft> tick;
    public Runnable onDisable;
    /** Optional: eigener Einstellungs-Screen (z.B. Partikel-Liste). Parameter = Elternscreen. */
    public Function<Screen, Screen> screen;

    public HudModule(String id, Item icon, int x, int y, boolean enabled, boolean hud) {
        this.id = id;
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.enabled = enabled;
        this.hud = hud;
        if (!hud) {
            // Werkzeug-Module haben nichts zu zeichnen: nur eigene Einstellungen zeigen
            settings.clear();
        }
    }

    public final <T extends Setting> T add(T s) {
        settings.add(s);
        return s;
    }

    public String name() {
        return I18n.tr("module." + id);
    }

    public void setEnabled(boolean on) {
        if (enabled && !on && onDisable != null) onDisable.run();
        enabled = on;
    }

    public void onTick(Minecraft mc) {
        if (tick != null) tick.accept(mc);
    }

    // ---------- Inhalt (von Unterklassen ueberschreibbar) ----------

    public String line() {
        String s = text == null ? "" : text.get();
        if (s.isEmpty() && editing) return name();
        return s;
    }

    protected int contentW(Minecraft mc) {
        return mc.font.width(line());
    }

    protected int contentH(Minecraft mc) {
        return 9;
    }

    protected void drawContent(GuiGraphics g, Minecraft mc, int color) {
        g.drawString(mc.font, line(), PAD, PAD, color, shadow.value);
    }

    protected boolean hidden(Minecraft mc) {
        return text != null && !editing && text.get().isEmpty();
    }

    // ---------- Darstellung ----------

    public float scaleF() {
        return scale.value / 100f;
    }

    public int width(Minecraft mc) {
        return (int) Math.ceil((contentW(mc) + 2 * PAD) * scaleF());
    }

    public int height(Minecraft mc) {
        return (int) Math.ceil((contentH(mc) + 2 * PAD) * scaleF());
    }

    public ItemStack iconStack() {
        return new ItemStack(icon);
    }

    protected int textColor() {
        if (rainbow.value) return 0xFF000000 | hsv((System.currentTimeMillis() % 4000) / 4000f);
        return 0xFF000000 | color.value;
    }

    private static int hsv(float h) {
        float f = h * 6f;
        int i = (int) f;
        f -= i;
        float q = 1f - f;
        float r, gr, b;
        switch (i % 6) {
            case 0 -> { r = 1; gr = f; b = 0; }
            case 1 -> { r = q; gr = 1; b = 0; }
            case 2 -> { r = 0; gr = 1; b = f; }
            case 3 -> { r = 0; gr = q; b = 1; }
            case 4 -> { r = f; gr = 0; b = 1; }
            default -> { r = 1; gr = 0; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (gr * 255) << 8) | (int) (b * 255);
    }

    public void render(GuiGraphics g, Minecraft mc) {
        if (!hud || hidden(mc)) return;
        int w = contentW(mc) + 2 * PAD;
        int h = contentH(mc) + 2 * PAD;
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        float s = scaleF();
        g.pose().scale(s, s);
        if (bg.value && bgAlpha.value > 0) {
            int a = Math.round(bgAlpha.value * 255 / 100f);
            g.fill(0, 0, w, h, (a << 24) | bgColor.value);
        }
        if (border.value) {
            int c = 0xFF000000 | borderColor.value;
            g.fill(0, 0, w, 1, c);
            g.fill(0, h - 1, w, h, c);
            g.fill(0, 0, 1, h, c);
            g.fill(w - 1, 0, w, h, c);
        }
        drawContent(g, mc, textColor());
        g.pose().popMatrix();
    }

    public boolean hit(Minecraft mc, double mx, double my) {
        return hud && mx >= x && mx <= x + width(mc) && my >= y && my <= y + height(mc);
    }
}
