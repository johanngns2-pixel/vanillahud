package de.vanillahud.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A button that follows the Theme settings (vanilla sprites or flat colors). */
public class ThemedButton extends AbstractWidget {
    public interface Press {
        void press(ThemedButton b);
    }

    private final Press press;
    private final boolean primary;

    protected ThemedButton(int x, int y, int w, int h, Component msg, Press press, boolean primary) {
        super(x, y, w, h, msg);
        this.press = press;
        this.primary = primary;
    }

    public static Builder builder(Component msg, Press press) {
        return new Builder(msg, press);
    }

    public static class Builder {
        private final Component msg;
        private final Press press;
        private int x, y, w = 150, h = 20;
        private boolean primary;

        Builder(Component msg, Press press) {
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
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        press.press(this);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        defaultButtonNarrationText(out);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Ui.button(g, Minecraft.getInstance().font, getX(), getY(), getWidth(), getHeight(), getMessage(),
                isHoveredOrFocused(), active, primary ? Theme.I.primary.get() : 0);
    }
}
