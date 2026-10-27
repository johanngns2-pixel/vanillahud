package de.vanillahud.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A vanilla button that can be recolored through the Theme settings. */
public class ThemedButton extends Button {
    private final boolean primary;

    protected ThemedButton(int x, int y, int w, int h, Component msg, OnPress onPress, boolean primary) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
        this.primary = primary;
    }

    public static Builder builder(Component msg, OnPress onPress) {
        return new Builder(msg, onPress);
    }

    public static class Builder {
        private final Component msg;
        private final OnPress press;
        private int x, y, w = 150, h = 20;
        private boolean primary;

        Builder(Component msg, OnPress press) {
            this.msg = msg;
            this.press = press;
        }

        public Builder bounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        public Builder primary() {
            this.primary = true;
            return this;
        }

        public ThemedButton build() {
            return new ThemedButton(x, y, w, h, msg, press, primary);
        }
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (!Theme.flat()) {
            super.renderWidget(g, mouseX, mouseY, delta);
            return;
        }
        Theme t = Theme.I;
        int bg = !active ? t.btnOff.get() : isHoveredOrFocused() ? t.btnHover.get() : primary ? t.primary.get() : t.btn.get();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        g.fill(x, y, x + w, y + h, t.btnBorder.get());
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        var font = net.minecraft.client.Minecraft.getInstance().font;
        g.drawCenteredString(font, getMessage(), x + w / 2, y + (h - 8) / 2, active ? t.btnText.get() : t.btnTextOff.get());
    }
}
