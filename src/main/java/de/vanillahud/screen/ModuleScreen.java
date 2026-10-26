package de.vanillahud.screen;

import de.vanillahud.module.Category;
import de.vanillahud.module.Module;
import de.vanillahud.module.Modules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Main menu: searchable, scrollable grid of module cards with category filter. */
public class ModuleScreen extends VhScreen {
    private static final int CW = 134, CH = 46, GAP = 6;

    private static String lastQuery = "";
    private static Category lastFilter = null;
    private static double lastScroll = 0;

    private EditBox search;
    private final List<Button> catButtons = new ArrayList<>();
    private final List<Module> shown = new ArrayList<>();
    private Category filter = lastFilter;
    private double scroll = lastScroll;
    private int vTop, vBottom, cols, left;

    public ModuleScreen() {
        super(Component.literal("VanillaHud Modules"));
    }

    @Override
    protected void init() {
        catButtons.clear();
        search = new EditBox(font, width / 2 - 100, 22, 200, 18, Component.literal("Search"));
        search.setMaxLength(40);
        search.setValue(lastQuery);
        search.setResponder(s -> {
            lastQuery = s;
            refilter();
        });
        addRenderableWidget(search);

        Category[] cats = {null, Category.INFO, Category.PVP, Category.VISUAL, Category.UTILITY};
        int bw = 62;
        int startX = width / 2 - (cats.length * (bw + 2)) / 2 + 1;
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            Button b = Button.builder(catLabel(c), btn -> {
                filter = c;
                lastFilter = c;
                scroll = 0;
                refilter();
                updateCatLabels();
            }).bounds(startX + i * (bw + 2), 44, bw, 18).build();
            catButtons.add(b);
            addRenderableWidget(b);
        }

        int by = height - 26;
        addRenderableWidget(Button.builder(Component.literal("Arrange HUD"), b -> minecraft.setScreen(new HudEditScreen(this)))
                .bounds(width / 2 - 206, by, 130, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reorder Modules"), b -> minecraft.setScreen(new OrderScreen(this)))
                .bounds(width / 2 - 65, by, 130, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 76, by, 130, 20).build());

        vTop = 68;
        vBottom = height - 32;
        refilter();
        updateCatLabels();
    }

    private Component catLabel(Category c) {
        String n = c == null ? "All" : c.label;
        return filter == c ? Component.literal(n).withStyle(ChatFormatting.YELLOW) : Component.literal(n);
    }

    private void updateCatLabels() {
        Category[] cats = {null, Category.INFO, Category.PVP, Category.VISUAL, Category.UTILITY};
        for (int i = 0; i < catButtons.size(); i++) catButtons.get(i).setMessage(catLabel(cats[i]));
    }

    private void refilter() {
        shown.clear();
        String q = lastQuery.toLowerCase().trim();
        for (Module m : Modules.ALL) {
            if (filter != null && m.category != filter) continue;
            if (!q.isEmpty() && !(m.name.toLowerCase().contains(q) || m.description.toLowerCase().contains(q) || m.id.contains(q))) continue;
            shown.add(m);
        }
        cols = Math.max(1, Math.min(6, (width - 24) / (CW + GAP)));
        left = (width - (cols * CW + (cols - 1) * GAP)) / 2;
        clampScroll();
    }

    private int contentH() {
        int rows = (shown.size() + cols - 1) / cols;
        return rows * (CH + GAP);
    }

    private double maxScroll() {
        return Math.max(0, contentH() - (vBottom - vTop));
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        lastScroll = scroll;
    }

    private int cardX(int i) {
        return left + (i % cols) * (CW + GAP);
    }

    private int cardY(int i) {
        return vTop + (i / cols) * (CH + GAP) - (int) scroll;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        if (search.getValue().isEmpty() && !search.isFocused()) {
            g.drawString(font, "Search modules...", search.getX() + 5, search.getY() + 5, 0xFF808080, false);
        }

        g.enableScissor(0, vTop, width, vBottom);
        for (int i = 0; i < shown.size(); i++) {
            Module m = shown.get(i);
            int x = cardX(i), y = cardY(i);
            if (y + CH < vTop || y > vBottom) continue;
            Ui.card(g, x, y, CW, CH, m.enabled);
            g.renderItem(new ItemStack(m.icon), x + 7, y + 4);
            g.drawString(font, font.plainSubstrByWidth(m.name, CW - 32), x + 28, y + 8, 0xFFFFFFFF, true);

            int bw = (CW - 14) / 2;
            boolean inView = my >= vTop && my < vBottom;
            Component state = Component.literal(m.enabled ? "ON" : "OFF").withStyle(m.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
            Ui.button(g, font, x + 5, y + 25, bw, 16, state, inView && Ui.in(mx, my, x + 5, y + 25, bw, 16), true);
            boolean hasSettings = !m.settings.isEmpty();
            Ui.button(g, font, x + 9 + bw, y + 25, bw, 16, Component.literal("Settings"),
                    inView && hasSettings && Ui.in(mx, my, x + 9 + bw, y + 25, bw, 16), hasSettings);
        }
        g.disableScissor();
        Ui.scrollbar(g, width - 8, vTop, vBottom, scroll, maxScroll(), Math.max(1, contentH()));
        if (shown.isEmpty()) g.drawCenteredString(font, "No modules found", width / 2, vTop + 20, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (my >= vTop && my < vBottom && event.button() == 0) {
            for (int i = 0; i < shown.size(); i++) {
                Module m = shown.get(i);
                int x = cardX(i), y = cardY(i);
                int bw = (CW - 14) / 2;
                if (Ui.in(mx, my, x + 5, y + 25, bw, 16)) {
                    m.setEnabled(!m.enabled);
                    Modules.save();
                    return true;
                }
                if (!m.settings.isEmpty() && Ui.in(mx, my, x + 9 + bw, y + 25, bw, 16)) {
                    minecraft.setScreen(new ModuleSettingsScreen(this, m));
                    return true;
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll -= sy * 20;
        clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT && !search.isFocused()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Modules.save();
        super.onClose();
    }
}
