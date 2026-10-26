package de.vanillahud.mixin;

import de.vanillahud.module.Modules;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    /** Zoom: Sichtfeld durch den Zoom-Faktor teilen. */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void vanillahud$zoom(Camera camera, float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        float z = Modules.zoomFactor();
        if (z > 1.001f) cir.setReturnValue(cir.getReturnValue() / z);
    }
}
