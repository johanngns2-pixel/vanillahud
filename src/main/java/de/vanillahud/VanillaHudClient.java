package de.vanillahud;

import com.mojang.blaze3d.platform.InputConstants;
import de.vanillahud.module.Features;
import de.vanillahud.module.HudModule;
import de.vanillahud.module.Module;
import de.vanillahud.module.Modules;
import de.vanillahud.screen.ModuleScreen;
import de.vanillahud.screen.VhScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class VanillaHudClient implements ClientModInitializer {
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        Modules.init();
        if (Modules.get("crosshair") instanceof Features.Crosshair c) c.register();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.vanillahud.menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenu.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new ModuleScreen());
            }
            Modules.tick(mc);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Modules.shutdown());

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            // our own screens draw the modules themselves; F1 hides the HUD
            if (mc.options.hideGui || mc.screen instanceof VhScreen) return;
            for (Module m : Modules.ALL) {
                if (m.enabled && m instanceof HudModule h) h.render(graphics, mc);
            }
        });
    }
}
