package de.vanillahud.screen;

import de.vanillahud.module.Module;
import de.vanillahud.module.Modules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Drag the modules in the list to change their order (menu order and HUD draw order). */
public class OrderScreen extends VhScreen {
    private static final int RH = 22;
    private final Screen parentScreen;
    private double scroll;
    private int vTop, vBottom, rowW, left;
    private Module dragging;
    private double dragY;

    public OrderScreen(Screen parent) {
        super(Component.literal("Reorder Modules"));
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        rowW = Math.min(280, width - 40);
        left = (width - rowW) / 2;
        vTop = 34;
        vBottom = height - 32;
        addRenderableWidget(ThemedButton.builder(Component.literal("Enabled First"), b -> {
            List<Module> on = new ArrayList<>(), off = new ArrayList<>();
            for (Module m : Modules.ALL) (m.enabled ? on : off).add(m);
            Modules.ALL.clear();
            Modules.ALL.addAll(on);
            Modules.ALL.addAll(off);
            Modules.save();
        }).bounds(width / 2 - 154, height - 26, 150, 20).build());
        addRenderableWidget(ThemedButton.builder(CommonComponents.GUI_DONE, b -> onClose()).primary()
                .bounds(width / 2 + 4, height - 26, 150, 20).build());
    }

    private double maxScroll() {
        return Math.max(0, Modules.ALL.size() * RH - (vBottom - vTop));
    }

    private int rowY(int i) {
        return vTop + i * RH - (int) scroll;
    }

    private void drawRow(GuiGraphics g, Module m, int y, boolean lifted) {
        Ui.card(g, left, y, rowW, RH - 2, m.enabled);
        if (lifted) g.renderOutline(left - 1, y - 1, rowW + 2, RH, 0xFFFFFF55);
        g.renderItem(new ItemStack(m.icon), left + 8, y + 1);
        g.drawString(font, m.name, left + 30, y + 6, 0xFFFFFFFF, true);
        // grip
        for (int k = 0; k < 3; k++) g.fill(left + rowW - 22, y + 5 + k * 4, left + rowW - 8, y + 6 + k * 4, 0xFFAAAAAA);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, title, width / 2, 8, Theme.I.title.get());
        g.drawCenteredString(font, "Drag a module up or down to move it", width / 2, 20, 0xFFAAAAAA);

        if (dragging != null) {
            if (my < vTop + 14) scroll -= 5;
            else if (my > vBottom - 14) scroll += 5;
            scroll = Math.max(0, Math.min(maxScroll(), scroll));
            int target = (int) Math.max(0, Math.min(Modules.ALL.size() - 1, (my + scroll - vTop) / RH));
            int cur = Modules.ALL.indexOf(dragging);
            if (target != cur && cur >= 0) {
                Modules.ALL.remove(cur);
                Modules.ALL.add(target, dragging);
            }
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll));

        g.enableScissor(0, vTop, width, vBottom);
        for (int i = 0; i < Modules.ALL.size(); i++) {
            Module m = Modules.ALL.get(i);
            if (m == dragging) continue;
            int y = rowY(i);
            if (y + RH < vTop || y > vBottom) continue;
            drawRow(g, m, y, false);
        }
        if (dragging != null) drawRow(g, dragging, (int) (dragY - RH / 2), true);
        g.disableScissor();
        Ui.scrollbar(g, left + rowW + 4, vTop, vBottom, scroll, maxScroll(), Math.max(1, Modules.ALL.size() * RH));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (event.button() == 0 && my >= vTop && my < vBottom && mx >= left && mx < left + rowW) {
            int i = (int) ((my + scroll - vTop) / RH);
            if (i >= 0 && i < Modules.ALL.size()) {
                dragging = Modules.ALL.get(i);
                dragY = my;
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            dragY = event.y();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            Modules.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - sy * 22));
        return true;
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parentScreen);
    }
}
