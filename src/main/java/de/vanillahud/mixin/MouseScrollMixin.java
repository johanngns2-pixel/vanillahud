package de.vanillahud.mixin;

import de.vanillahud.module.ZoomModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseScrollMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void vanillahud$scroll(long window, double xoffset, double yoffset, CallbackInfo ci) {
        if (ZoomModule.onScroll(yoffset)) ci.cancel();
    }
}
