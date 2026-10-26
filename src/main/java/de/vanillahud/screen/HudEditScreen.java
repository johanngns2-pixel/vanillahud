package de.vanillahud.screen;

import de.vanillahud.module.HudModule;
import de.vanillahud.module.Modules;
import de.vanillahud.util.I18n;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Module mit der Maus ziehen, Rechtsklick = Einstellungen. ESC / Right Shift = zurueck. */
public class HudEditScreen extends Screen {
    private final Screen parent;
    private HudModule dragging;
    private double offX, offY;

    public HudEditScreen(Screen parent) {
        super(Component.literal("HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HudModule.editing = true;
    }

    @Override
    public void removed() {
        HudModule.editing = false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, I18n.tr("ui.dragHint"), width / 2, 8, 0xFFFFFFFF);

        for (HudModule m : Modules.ALL) {
            if (!m.hud || !m.enabled) continue;
            m.render(g, minecraft);
            int w = m.width(minecraft);
            int h = m.height(minecraft);
            int color = (m == dragging || m.hit(minecraft, mouseX, mouseY)) ? 0xFFFFFF55 : 0xFFAAAAAA;
            g.fill(m.x - 1, m.y - 1, m.x + w + 1, m.y, color);
            g.fill(m.x - 1, m.y + h, m.x + w + 1, m.y + h + 1, color);
            g.fill(m.x - 1, m.y, m.x, m.y + h, color);
            g.fill(m.x + w, m.y, m.x + w + 1, m.y + h, color);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
            HudModule m = Modules.ALL.get(i);
            if (m.hud && m.enabled && m.hit(minecraft, event.x(), event.y())) {
                if (event.button() == 1) {
                    minecraft.setScreen(new ModuleSettingsScreen(this, m));
                    return true;
                }
                if (event.button() == 0) {
                    dragging = m;
                    offX = event.x() - m.x;
                    offY = event.y() - m.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            int nx = (int) (event.x() - offX);
            int ny = (int) (event.y() - offY);
            dragging.x = Math.max(0, Math.min(width - dragging.width(minecraft), nx));
            dragging.y = Math.max(0, Math.min(height - dragging.height(minecraft), ny));
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
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parent);
    }
}
