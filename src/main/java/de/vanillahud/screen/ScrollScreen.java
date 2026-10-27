package de.vanillahud.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Basis fuer Screens mit einer scrollbaren Liste (Mausrad). Die Listen-Widgets werden hier selbst
 * gezeichnet (mit Scissor) und mit ihrer logischen Y-Position relativ zum Listenanfang abgelegt.
 */
public abstract class ScrollScreen extends Screen {
    protected int listTop, listBottom;
    protected double scroll;
    protected int contentH;
    private final List<AbstractWidget> items = new ArrayList<>();
    private final List<Integer> itemY = new ArrayList<>();

    protected ScrollScreen(Component title) {
        super(title);
    }

    protected void clearItems() {
        items.clear();
        itemY.clear();
    }

    protected <T extends AbstractWidget> T addItem(T w, int logicalY) {
        items.add(w);
        itemY.add(logicalY);
        return w;
    }

    protected int viewH() {
        return listBottom - listTop;
    }

    protected void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentH - viewH())));
    }

    /** Hintergrund/Karten der Liste zeichnen. top = Bildschirm-Y des Listenanfangs (schon um den Scroll verschoben). */
    protected abstract void renderRows(GuiGraphics g, int mouseX, int mouseY, float delta, int top);

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        clampScroll();
        int top = listTop - (int) scroll;
        g.enableScissor(0, listTop, width, listBottom);
        renderRows(g, mouseX, mouseY, delta, top);
        for (int i = 0; i < items.size(); i++) {
            AbstractWidget w = items.get(i);
            w.setY(top + itemY.get(i));
            w.render(g, mouseX, mouseY, delta);
        }
        g.disableScissor();

        if (contentH > viewH()) {
            int barX = width - 6;
            int barH = Math.max(16, viewH() * viewH() / contentH);
            int barY = listTop + (int) ((viewH() - barH) * (scroll / (contentH - viewH())));
            g.fill(barX, listTop, barX + 4, listBottom, 0x55000000);
            g.fill(barX, barY, barX + 4, barY + barH, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (my >= listTop && my <= listBottom) {
            scroll -= sy * 20;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.y() >= listTop && event.y() <= listBottom) {
            for (AbstractWidget w : new ArrayList<>(items)) {
                if (w.visible && w.mouseClicked(event, doubleClick)) return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
