package de.vanillahud.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Marker base class for all screens of this mod (the HUD is hidden behind them). */
public abstract class VhScreen extends Screen {
    protected VhScreen(Component title) {
        super(title);
    }
}
