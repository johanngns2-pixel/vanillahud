package de.vanillahud.module;

import de.vanillahud.setting.Setting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;

public class KeystrokesModule extends HudModule {
    public final Setting.Num size = add(new Setting.Num("size", "Key Size", 16, 34, 1, 22, "px"));
    public final Setting.Num gap = add(new Setting.Num("gap", "Key Gap", 0, 6, 1, 2, "px"));
    public final Setting.Bool space = add(new Setting.Bool("space", "Show Space Bar", true));
    public final Setting.Bool mouse = add(new Setting.Bool("mouse", "Show Mouse Buttons", true));
    public final Setting.Bool cps = add(new Setting.Bool("cps", "Show CPS", true));
    public final Setting.Color keyColor = add(new Setting.Color("key_color", "Key Color", 0x80000000, true, false));
    public final Setting.Color pressedColor = add(new Setting.Color("pressed_color", "Pressed Color", 0xB0FFFFFF, true, false));
    public final Setting.Color pressedText = add(new Setting.Color("pressed_text", "Pressed Text Color", 0xFF000000));

    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", "Shows WASD, space and mouse buttons.", Items.OAK_BUTTON, Category.INFO, false);
        background.value = false;
        padding.value = 0;
    }

    @Override
    protected int contentWidth(Minecraft mc) {
        return 3 * size.i() + 2 * gap.i();
    }

    @Override
    protected int contentHeight(Minecraft mc) {
        int s = size.i(), g = gap.i();
        int h = 2 * s + g;
        if (space.value) h += g + s / 2;
        if (mouse.value) h += g + s;
        return h;
    }

    private void key(GuiGraphics g, Minecraft mc, int x, int y, int w, int h, String label, boolean down, int color, String sub) {
        g.fill(x, y, x + w, y + h, down ? pressedColor.get() : keyColor.get());
        int c = down ? pressedText.get() : color;
        if (sub == null) {
            g.drawCenteredString(mc.font, label, x + w / 2, y + (h - 8) / 2, c);
        } else {
            g.drawCenteredString(mc.font, label, x + w / 2, y + 3, c);
            g.drawCenteredString(mc.font, sub, x + w / 2, y + h - 11, c);
        }
    }

    @Override
    protected void drawContent(GuiGraphics g, Minecraft mc, int cx, int cy, int color) {
        int s = size.i(), gp = gap.i();
        var o = mc.options;
        key(g, mc, cx + s + gp, cy, s, s, "W", o.keyUp.isDown(), color, null);
        int y2 = cy + s + gp;
        key(g, mc, cx, y2, s, s, "A", o.keyLeft.isDown(), color, null);
        key(g, mc, cx + s + gp, y2, s, s, "S", o.keyDown.isDown(), color, null);
        key(g, mc, cx + 2 * (s + gp), y2, s, s, "D", o.keyRight.isDown(), color, null);
        int y = y2 + s + gp;
        if (space.value) {
            key(g, mc, cx, y, contentWidth(mc), s / 2, "SPACE", o.keyJump.isDown(), color, null);
            y += s / 2 + gp;
        }
        if (mouse.value) {
            int w = (contentWidth(mc) - gp) / 2;
            KeyMapping a = o.keyAttack, u = o.keyUse;
            key(g, mc, cx, y, w, s, "LMB", a.isDown(), color, cps.value ? Modules.cpsLeft() + " CPS" : null);
            key(g, mc, cx + w + gp, y, w, s, "RMB", u.isDown(), color, cps.value ? Modules.cpsRight() + " CPS" : null);
        }
    }
}
