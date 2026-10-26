package de.vanillahud.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/** Ein verschiebbares HUD-Modul. Der Text wird pro Frame ueber den Supplier erzeugt. */
public class HudModule {
    public final String id;
    public final String name;
    public final Item icon;
    private final Supplier<String> text;

    public boolean enabled;
    public int x, y;

    public HudModule(String id, String name, Item icon, int x, int y, boolean enabled, Supplier<String> text) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.enabled = enabled;
        this.text = text;
    }

    public String line() {
        return text == null ? name : text.get();
    }

    public int width(Minecraft mc) {
        return mc.font.width(line()) + 8;
    }

    public int height() {
        return 16;
    }

    public ItemStack iconStack() {
        return new ItemStack(icon);
    }

    /** Vanilla-Look: halbtransparenter dunkler Kasten + Text mit Schatten. */
    public void render(GuiGraphics g, Minecraft mc) {
        int w = width(mc);
        g.fill(x, y, x + w, y + height(), 0x90000000);
        g.drawString(mc.font, line(), x + 4, y + 4, 0xFFFFFFFF, true);
    }

    public boolean hit(Minecraft mc, double mx, double my) {
        return mx >= x && mx <= x + width(mc) && my >= y && my <= y + height();
    }
}
