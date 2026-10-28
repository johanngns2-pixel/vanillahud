package de.vanillahud.screen;

import de.vanillahud.module.HudModule;
import de.vanillahud.module.Module;
import de.vanillahud.module.Modules;
import de.vanillahud.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Scrollable settings list of one module with live preview. */
public class ModuleSettingsScreen extends VhScreen {
    private static final int RH = 20, RGAP = 3;

    private final net.minecraft.client.gui.screens.Screen parentScreen;
    private final Module module;
    private EditBox search;
    private String query = "";
    private double scroll;
    private int vTop, vBottom, rowW, left, previewH;
    private Setting.Num dragging;
    private boolean hasSearch;
    private boolean showInfo;

    public ModuleSettingsScreen(net.minecraft.client.gui.screens.Screen parent, Module module) {
        super(Component.literal(module.name + " Settings"));
        this.parentScreen = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        rowW = Math.min(300, width - 30);
        left = (width - rowW) / 2;
        previewH = 0;
        if (module instanceof HudModule h) previewH = Math.min(100, Math.max(24, h.height(minecraft) + 10));
        vTop = 22 + (previewH > 0 ? previewH + 4 : 0);
        hasSearch = module.settings.size() > 10;
        if (hasSearch) {
            search = new EditBox(font, left, vTop, rowW, 18, Component.literal("Search"));
            search.setValue(query);
            search.setResponder(s -> {
                query = s;
                scroll = 0;
            });
            addRenderableWidget(search);
            vTop += 22;
        }
        vBottom = height - 30;
        addRenderableWidget(ThemedButton.builder(Component.literal("Info"), b -> showInfo = !showInfo)
                .bounds(width - 66, 6, 60, 16).build());
        addRenderableWidget(ThemedButton.builder(Component.literal("Reset Settings"), b -> {
            module.resetSettings();
            Modules.save();
        }).bounds(width / 2 - 154, height - 26, 150, 20).build());
        addRenderableWidget(ThemedButton.builder(CommonComponents.GUI_DONE, b -> onClose()).primary()
                .bounds(width / 2 + 4, height - 26, 150, 20).build());
    }

    private List<Setting> rows() {
        List<Setting> l = new ArrayList<>();
        String q = query.toLowerCase().trim();
        for (Setting s : module.settings) {
            if (!s.visible()) continue;
            if (!q.isEmpty() && !s.name.toLowerCase().contains(q)) continue;
            l.add(s);
        }
        return l;
    }

    // row 0 is the "Enabled" toggle (only without a search query)
    private int totalRows(List<Setting> l) {
        return l.size() + (hasEnable() ? 1 : 0);
    }

    private boolean hasEnable() {
        return query.isEmpty() && module.toggleable();
    }

    private int contentH(List<Setting> l) {
        return totalRows(l) * (RH + RGAP);
    }

    private double maxScroll(List<Setting> l) {
        return Math.max(0, contentH(l) - (vBottom - vTop));
    }

    private int rowY(int idx) {
        return vTop + idx * (RH + RGAP) - (int) scroll;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, title, width / 2, 8, Theme.I.title.get());

        if (module instanceof HudModule h) {
            g.fill(left, 22, left + rowW, 22 + previewH, 0xFF202020);
            g.renderOutline(left, 22, rowW, previewH, 0xFF606060);
            int pw = h.width(minecraft);
            int ph = h.height(minecraft);
            h.render(g, minecraft, left + Math.max(2, (rowW - pw) / 2), 22 + Math.max(2, (previewH - ph) / 2));
        }
        if (hasSearch && search.getValue().isEmpty() && !search.isFocused()) {
            g.drawString(font, "Search settings...", search.getX() + 5, search.getY() + 5, 0xFF808080, false);
        }

        List<Setting> rows = rows();
        scroll = Math.max(0, Math.min(maxScroll(rows), scroll));
        g.enableScissor(0, vTop, width, vBottom);
        int idx = 0;
        if (hasEnable()) {
            int y = rowY(idx++);
            if (y + RH > vTop && y < vBottom) {
                Component c = Component.literal("Enabled: ").append(Ui.onOff(module.enabled));
                Ui.button(g, font, left, y, rowW, RH, c, inRow(mx, my, y), true);
            }
        }
        for (Setting s : rows) {
            int y = rowY(idx++);
            if (y + RH <= vTop || y >= vBottom) continue;
            boolean hov = inRow(mx, my, y);
            drawRow(g, s, y, hov);
        }
        g.disableScissor();
        if (showInfo) {
            var lines = font.split(Component.literal(module.description), rowW - 12);
            int ih = lines.size() * 10 + 24;
            int iy = vTop + 4;
            g.fill(left, iy, left + rowW, iy + ih, 0xF0101010);
            g.renderOutline(left, iy, rowW, ih, 0xFFA0A0A0);
            g.drawString(font, module.name, left + 6, iy + 5, Theme.I.title.get(), true);
            for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), left + 6, iy + 18 + i * 10, 0xFFDDDDDD, false);
        }
        Ui.scrollbar(g, left + rowW + 4, vTop, vBottom, scroll, maxScroll(rows), Math.max(1, contentH(rows)));
    }

    private boolean inRow(int mx, int my, int y) {
        return my >= vTop && my < vBottom && Ui.in(mx, my, left, y, rowW, RH);
    }

    private void drawRow(GuiGraphics g, Setting s, int y, boolean hov) {
        if (s instanceof Setting.Bool b) {
            Component c = Component.literal(b.name + ": ").append(Ui.onOff(b.value));
            Ui.button(g, font, left, y, rowW, RH, c, hov, true);
        } else if (s instanceof Setting.Num n) {
            double frac = (n.value - n.min) / (n.max - n.min);
            Ui.slider(g, font, left, y, rowW, RH, Component.literal(n.name + ": " + n.text()), frac, hov || dragging == n);
        } else if (s instanceof Setting.Choice c) {
            Ui.button(g, font, left, y, rowW, RH, Component.literal(c.name + ": " + c.get()), hov, true);
        } else if (s instanceof Setting.Color c) {
            Ui.button(g, font, left, y, rowW, RH, Component.literal(c.name + (c.chroma ? "  (Chroma)" : "")), hov, true);
            int sx = left + rowW - 30, sy = y + 4;
            g.fill(sx - 1, sy - 1, sx + 25, sy + 13, 0xFF000000);
            g.fill(sx, sy, sx + 24, sy + 12, 0xFF555555);
            g.fill(sx, sy, sx + 24, sy + 12, c.get());
        } else if (s instanceof Setting.Action a) {
            Ui.button(g, font, left, y, rowW, RH, Component.literal(a.name), hov, true);
        }
    }

    private void setFromMouse(Setting.Num n, double mx) {
        double frac = (mx - (left + 4)) / (rowW - 8);
        frac = Math.max(0, Math.min(1, frac));
        n.set(n.min + frac * (n.max - n.min));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (my >= vTop && my < vBottom && mx >= left && mx < left + rowW) {
            List<Setting> rows = rows();
            int idx = 0;
            if (hasEnable()) {
                int y = rowY(idx++);
                if (my >= y && my < y + RH) {
                    module.setEnabled(!module.enabled);
                    Modules.save();
                    return true;
                }
            }
            for (Setting s : rows) {
                int y = rowY(idx++);
                if (my < y || my >= y + RH) continue;
                int btn = event.button();
                if (s instanceof Setting.Bool b) {
                    b.value = !b.value;
                } else if (s instanceof Setting.Num n) {
                    dragging = n;
                    setFromMouse(n, mx);
                } else if (s instanceof Setting.Choice c) {
                    c.cycle(btn == 1 ? -1 : 1);
                } else if (s instanceof Setting.Color c) {
                    minecraft.setScreen(new ColorPickerScreen(this, c));
                } else if (s instanceof Setting.Action a) {
                    a.run.run();
                }
                Modules.save();
                return true;
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            setFromMouse(dragging, event.x());
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
        scroll -= sy * 20;
        scroll = Math.max(0, Math.min(maxScroll(rows()), scroll));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT && !(hasSearch && search.isFocused())) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parentScreen);
    }
}
