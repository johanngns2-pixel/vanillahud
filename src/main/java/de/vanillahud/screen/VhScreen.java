package de.vanillahud.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Marker base class for all screens of this mod (the HUD is hidden behind them). */
public abstract class VhScreen extends Screen {
    protected VhScreen(Component title) {
        super(title);
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float delta) {
        super.renderBackground(g, mx, my, delta);
        int tint = Theme.I.bgTint.get();
        if ((tint >>> 24) != 0) g.fill(0, 0, width, height, tint);
    }
}
