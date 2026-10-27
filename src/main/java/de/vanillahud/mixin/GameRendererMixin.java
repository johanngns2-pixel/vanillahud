package de.vanillahud.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.vanillahud.module.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    /** Only modifies the value (never cancels), so other zoom mods and FOV mods keep working. */
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"), require = 0)
    private float vanillahud$zoom(float original, Camera camera, float partialTick, boolean useFovSetting) {
        return useFovSetting ? ZoomModule.apply(original) : original;
    }
}
