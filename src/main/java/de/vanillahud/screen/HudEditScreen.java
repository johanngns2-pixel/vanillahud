package de.vanillahud.screen;

import de.vanillahud.module.HudModule;
import de.vanillahud.module.Module;
import de.vanillahud.module.Modules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Drag modules to the wanted place. Right click a module to open its settings. */
public class HudEditScreen extends VhScreen {
    private final Screen parentScreen;
    private HudModule dragging;
    private double offX, offY;

    public HudEditScreen(Screen parent) {
        super(Component.literal("Arrange HUD"));
        this.parentScreen = parent;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, "Drag modules  |  Right click: settings  |  Arrow keys: nudge  |  Right Shift / Esc: back", width / 2, 8, 0xFFFFFFFF);

        for (Module mod : Modules.ALL) {
            if (!(mod instanceof HudModule m) || !m.enabled) continue;
            m.render(g, minecraft);
            int w = m.width(minecraft), h = m.height(minecraft);
            int color = (m == dragging || m.hit(minecraft, mouseX, mouseY)) ? 0xFFFFFF55 : 0xFFAAAAAA;
            g.renderOutline(m.x - 1, m.y - 1, w + 2, h + 2, color);
        }
        if (dragging != null) {
            // center guides
            g.fill(width / 2, 0, width / 2 + 1, height, 0x40FFFFFF);
            g.fill(0, height / 2, width, height / 2 + 1, 0x40FFFFFF);
        }
    }

    private HudModule at(double mx, double my) {
        for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
            if (Modules.ALL.get(i) instanceof HudModule m && m.enabled && m.hit(minecraft, mx, my)) return m;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        HudModule m = at(event.x(), event.y());
        if (m != null) {
            if (event.button() == 0) {
                dragging = m;
                offX = event.x() - m.x;
                offY = event.y() - m.y;
                return true;
            }
            if (event.button() == 1) {
                minecraft.setScreen(new ModuleSettingsScreen(this, m));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            int w = dragging.width(minecraft), h = dragging.height(minecraft);
            int nx = (int) (event.x() - offX);
            int ny = (int) (event.y() - offY);
            // snap to center and edges
            if (Math.abs(nx + w / 2 - width / 2) < 4) nx = width / 2 - w / 2;
            if (Math.abs(ny + h / 2 - height / 2) < 4) ny = height / 2 - h / 2;
            if (Math.abs(nx) < 4) nx = 0;
            if (Math.abs(ny) < 4) ny = 0;
            if (Math.abs(nx + w - width) < 4) nx = width - w;
            if (Math.abs(ny + h - height) < 4) ny = height - h;
            dragging.x = Math.max(0, Math.min(width - w, nx));
            dragging.y = Math.max(0, Math.min(height - h, ny));
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
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        if (k == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        HudModule m = dragging;
        if (m == null) {
            double mx = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
            double my = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
            m = at(mx, my);
        }
        if (m != null) {
            if (k == GLFW.GLFW_KEY_LEFT) { m.x--; return true; }
            if (k == GLFW.GLFW_KEY_RIGHT) { m.x++; return true; }
            if (k == GLFW.GLFW_KEY_UP) { m.y--; return true; }
            if (k == GLFW.GLFW_KEY_DOWN) { m.y++; return true; }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parentScreen);
    }
}
