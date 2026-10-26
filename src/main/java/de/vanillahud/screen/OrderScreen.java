package de.vanillahud.screen;

import de.vanillahud.module.Module;
import de.vanillahud.module.Modules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Change the order of the modules (menu order and HUD draw order). */
public class OrderScreen extends VhScreen {
    private static final int RH = 22;
    private final Screen parentScreen;
    private double scroll;
    private int vTop, vBottom, rowW, left;

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
        addRenderableWidget(Button.builder(Component.literal("Enabled First"), b -> {
            List<Module> on = new ArrayList<>(), off = new ArrayList<>();
            for (Module m : Modules.ALL) (m.enabled ? on : off).add(m);
            Modules.ALL.clear();
            Modules.ALL.addAll(on);
            Modules.ALL.addAll(off);
            Modules.save();
        }).bounds(width / 2 - 154, height - 26, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 4, height - 26, 150, 20).build());
    }

    private double maxScroll() {
        return Math.max(0, Modules.ALL.size() * RH - (vBottom - vTop));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        g.drawCenteredString(font, "Use the arrows to move a module up or down", width / 2, 20, 0xFFAAAAAA);
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        g.enableScissor(0, vTop, width, vBottom);
        for (int i = 0; i < Modules.ALL.size(); i++) {
            Module m = Modules.ALL.get(i);
            int y = vTop + i * RH - (int) scroll;
            if (y + RH < vTop || y > vBottom) continue;
            Ui.card(g, left, y, rowW, RH - 2, m.enabled);
            g.renderItem(new ItemStack(m.icon), left + 6, y + 1);
            g.drawString(font, m.name, left + 28, y + 6, 0xFFFFFFFF, true);
            boolean ok = my >= vTop && my < vBottom;
            Ui.button(g, font, left + rowW - 44, y + 1, 20, 18, Component.literal("^"), ok && Ui.in(mx, my, left + rowW - 44, y + 1, 20, 18), i > 0);
            Ui.button(g, font, left + rowW - 22, y + 1, 20, 18, Component.literal("v"), ok && Ui.in(mx, my, left + rowW - 22, y + 1, 20, 18), i < Modules.ALL.size() - 1);
        }
        g.disableScissor();
        Ui.scrollbar(g, left + rowW + 4, vTop, vBottom, scroll, maxScroll(), Math.max(1, Modules.ALL.size() * RH));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (event.button() == 0 && my >= vTop && my < vBottom) {
            for (int i = 0; i < Modules.ALL.size(); i++) {
                int y = vTop + i * RH - (int) scroll;
                if (Ui.in(mx, my, left + rowW - 44, y + 1, 20, 18)) {
                    Modules.move(Modules.ALL.get(i), -1);
                    return true;
                }
                if (Ui.in(mx, my, left + rowW - 22, y + 1, 20, 18)) {
                    Modules.move(Modules.ALL.get(i), 1);
                    return true;
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
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
