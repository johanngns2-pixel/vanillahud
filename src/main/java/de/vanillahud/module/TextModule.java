package de.vanillahud.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Function;

/** HUD module that shows one or more lines of text. */
public class TextModule extends HudModule {
    public Function<Minecraft, List<String>> lines;
    /** Optional color override (ARGB), e.g. green/yellow/red by value. */
    public Function<Minecraft, Integer> tint;

    public TextModule(String id, String name, String description, Item icon, Category cat, boolean enabled) {
        super(id, name, description, icon, cat, enabled);
    }

    public TextModule single(Function<Minecraft, String> f) {
        this.lines = mc -> List.of(f.apply(mc));
        return this;
    }

    public TextModule multi(Function<Minecraft, List<String>> f) {
        this.lines = f;
        return this;
    }

    private List<String> safe(Minecraft mc) {
        try {
            List<String> l = lines.apply(mc);
            return l == null || l.isEmpty() ? List.of("-") : l;
        } catch (Exception e) {
            return List.of("-");
        }
    }

    @Override
    protected int contentWidth(Minecraft mc) {
        int w = 0;
        for (String s : safe(mc)) w = Math.max(w, mc.font.width(s));
        return w;
    }

    @Override
    protected int contentHeight(Minecraft mc) {
        return safe(mc).size() * 10 - 1;
    }

    @Override
    protected void drawContent(GuiGraphics g, Minecraft mc, int cx, int cy, int color) {
        int i = 0;
        Integer t = null;
        try {
            t = tint == null ? null : tint.apply(mc);
        } catch (Exception ignored) {
        }
        for (String s : safe(mc)) {
            int c = t != null ? t : textColor.chroma ? textColor.get(i * 0.08f) : color;
            g.drawString(mc.font, s, cx, cy + i * 10, c, shadow.value);
            i++;
        }
    }
}
