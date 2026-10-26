package de.vanillahud.module.impl;

import de.vanillahud.module.HudModule;
import de.vanillahud.setting.BoolSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;

import java.util.Locale;

/** WASD + Maustasten + Leertaste, gedrueckte Tasten leuchten auf. */
public class KeystrokesModule extends HudModule {
    private static final int S = 18, GAP = 2;
    public final BoolSetting showMouse = add(new BoolSetting("showMouse", true));
    public final BoolSetting showSpace = add(new BoolSetting("showSpace", true));

    public KeystrokesModule(int x, int y, boolean on) {
        super("keystrokes", Items.OAK_BUTTON, x, y, on, true);
    }

    @Override
    protected int contentW(Minecraft mc) {
        return 3 * S + 2 * GAP;
    }

    @Override
    protected int contentH(Minecraft mc) {
        int h = 2 * S + GAP;
        if (showMouse.value) h += S + GAP;
        if (showSpace.value) h += 10 + GAP;
        return h;
    }

    private static String label(KeyMapping k) {
        String s = k.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
        return s.length() > 3 ? s.substring(0, 3) : s;
    }

    private void key(GuiGraphics g, Minecraft mc, int bx, int by, int w, int h, String label, boolean down, int color) {
        g.fill(PAD + bx, PAD + by, PAD + bx + w, PAD + by + h, down ? 0xB0FFFFFF : 0x70000000);
        int tw = mc.font.width(label);
        g.drawString(mc.font, label, PAD + bx + (w - tw) / 2, PAD + by + (h - 8) / 2, down ? 0xFF000000 : color, false);
    }

    @Override
    protected void drawContent(GuiGraphics g, Minecraft mc, int color) {
        var o = mc.options;
        key(g, mc, S + GAP, 0, S, S, label(o.keyUp), o.keyUp.isDown(), color);
        key(g, mc, 0, S + GAP, S, S, label(o.keyLeft), o.keyLeft.isDown(), color);
        key(g, mc, S + GAP, S + GAP, S, S, label(o.keyDown), o.keyDown.isDown(), color);
        key(g, mc, 2 * (S + GAP), S + GAP, S, S, label(o.keyRight), o.keyRight.isDown(), color);
        int yy = 2 * S + 2 * GAP;
        int half = (3 * S + 2 * GAP - GAP) / 2;
        if (showMouse.value) {
            key(g, mc, 0, yy, half, S, "LMB", o.keyAttack.isDown(), color);
            key(g, mc, half + GAP, yy, half, S, "RMB", o.keyUse.isDown(), color);
            yy += S + GAP;
        }
        if (showSpace.value) {
            key(g, mc, 0, yy, 3 * S + 2 * GAP, 10, "-----", o.keyJump.isDown(), color);
        }
    }
}
