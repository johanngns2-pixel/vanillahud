package de.vanillahud.mixin;

import de.vanillahud.module.Modules;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    /** Block-Outline: Farbe (ARGB) der Umrandung des angeschauten Blocks. */
    @ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int vanillahud$outlineColor(int color) {
        return Modules.outlineColor(color);
    }

    /** Block-Outline: Liniendicke. */
    @ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float vanillahud$outlineWidth(float width) {
        return Modules.outlineWidth(width);
    }
}
