package de.vanillahud.screen;

import de.vanillahud.module.HudModule;
import de.vanillahud.module.Modules;
import de.vanillahud.util.I18n;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reihenfolge der eingeschalteten HUD-Module aendern und per Knopf untereinander stapeln. */
public class OrderScreen extends ScrollScreen {
    private static final int ROW_H = 24;
    private final Screen parent;
    private final List<HudModule> rows = new ArrayList<>();

    public OrderScreen(Screen parent) {
        super(Component.literal("Order"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        listTop = 40;
        listBottom = height - 34;
        int by = height - 28;
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.stack")), b -> {
            stack();
            Modules.save();
        }).bounds(width / 2 - 154, by, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.back")), b -> onClose())
                .bounds(width / 2 + 4, by, 150, 20).build());
        buildList();
    }

    private void buildList() {
        clearItems();
        rows.clear();
        for (HudModule m : Modules.ALL) if (m.hud && m.enabled) rows.add(m);
        int left = width / 2 - 130;
        for (int i = 0; i < rows.size(); i++) {
            HudModule m = rows.get(i);
            Button up = Button.builder(Component.literal("^"), b -> move(m, -1)).bounds(left + 200, 0, 28, 18).build();
            Button down = Button.builder(Component.literal("v"), b -> move(m, 1)).bounds(left + 230, 0, 28, 18).build();
            up.active = i > 0;
            down.active = i < rows.size() - 1;
            addItem(up, i * ROW_H + 2);
            addItem(down, i * ROW_H + 2);
        }
        contentH = rows.size() * ROW_H;
    }

    private void move(HudModule m, int dir) {
        int i = rows.indexOf(m);
        int j = i + dir;
        if (j < 0 || j >= rows.size()) return;
        Collections.swap(Modules.ALL, Modules.ALL.indexOf(m), Modules.ALL.indexOf(rows.get(j)));
        Modules.save();
        buildList();
    }

    /** Alle eingeschalteten Module in der gewaehlten Reihenfolge untereinander am linken Rand anordnen. */
    private void stack() {
        int x = 4, y = 4, colW = 0;
        for (HudModule m : rows) {
            int h = m.height(minecraft), w = m.width(minecraft);
            if (y + h > height - 4 && y > 4) {
                x += colW + 6;
                y = 4;
                colW = 0;
            }
            m.x = x;
            m.y = y;
            y += h + 2;
            colW = Math.max(colW, w);
        }
    }

    @Override
    protected void renderRows(GuiGraphics g, int mouseX, int mouseY, float delta, int top) {
        int left = width / 2 - 130;
        if (rows.isEmpty()) g.drawCenteredString(font, I18n.tr("ui.noEnabled"), width / 2, listTop + 20, 0xFFAAAAAA);
        for (int i = 0; i < rows.size(); i++) {
            HudModule m = rows.get(i);
            int y = top + i * ROW_H;
            g.fill(left, y + 1, left + 260, y + ROW_H - 1, 0xFF373737);
            g.renderItem(m.iconStack(), left + 4, y + 4);
            g.drawString(font, (i + 1) + ". " + m.name(), left + 26, y + 8, 0xFFFFFFFF, true);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, I18n.tr("ui.orderTitle"), width / 2, 12, 0xFFFFFFFF);
        g.drawCenteredString(font, I18n.tr("ui.orderHint"), width / 2, 26, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parent);
    }
}
