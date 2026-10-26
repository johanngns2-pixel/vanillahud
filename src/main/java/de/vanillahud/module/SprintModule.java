package de.vanillahud.module;

import de.vanillahud.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;

/** Toggle sprint / toggle sneak with an optional status text. */
public class SprintModule extends HudModule {
    public final Setting.Choice sprint = add(new Setting.Choice("sprint", "Sprint Mode", 1, "Hold", "Toggle", "Always"));
    public final Setting.Bool sneak = add(new Setting.Bool("toggle_sneak", "Toggle Sneak", false));
    public final Setting.Bool status = add(new Setting.Bool("status", "Show Status Text", true));

    private boolean sprintOn, sneakOn;

    public SprintModule() {
        super("toggle_sprint", "Toggle Sprint", "Sprint and sneak without holding the key.", Items.LEATHER_BOOTS, Category.UTILITY, false);
        background.value = false;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.screen != null) return;
        var sp = mc.options.keySprint;
        var sn = mc.options.keyShift;
        switch (sprint.get()) {
            case "Toggle" -> {
                while (sp.consumeClick()) sprintOn = !sprintOn;
                sp.setDown(sprintOn);
            }
            case "Always" -> {
                sprintOn = true;
                sp.setDown(true);
            }
            default -> sprintOn = false;
        }
        if (sneak.value) {
            while (sn.consumeClick()) sneakOn = !sneakOn;
            sn.setDown(sneakOn);
        } else {
            sneakOn = false;
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (sprintOn) mc.options.keySprint.setDown(false);
        if (sneakOn) mc.options.keyShift.setDown(false);
        sprintOn = false;
        sneakOn = false;
    }

    private String text(Minecraft mc) {
        if (!status.value) return "";
        if (sneakOn) return "Sneaking (Toggled)";
        if (mc.player != null && mc.player.isSprinting()) {
            return sprint.is("Always") ? "Sprinting (Auto)" : sprint.is("Toggle") ? "Sprinting (Toggled)" : "Sprinting";
        }
        if (sprintOn && sprint.is("Toggle")) return "Sprint: ON";
        return "Walking";
    }

    @Override
    protected int contentWidth(Minecraft mc) {
        return Math.max(10, mc.font.width(text(mc)));
    }

    @Override
    protected int contentHeight(Minecraft mc) {
        return 9;
    }

    @Override
    protected void drawContent(GuiGraphics g, Minecraft mc, int cx, int cy, int color) {
        g.drawString(mc.font, text(mc), cx, cy, color, shadow.value);
    }
}
