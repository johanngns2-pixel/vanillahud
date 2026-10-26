package de.vanillahud.mixin;

import de.vanillahud.module.Modules;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    // Seit 1.21.9: onButton(long window, MouseButtonInfo info, int action)
    @Inject(method = "onButton", at = @At("HEAD"))
    private void vanillahud$click(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (action == 1) Modules.click(info.button());
    }
}
