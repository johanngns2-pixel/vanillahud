package de.vanillahud.mixin;

import de.vanillahud.module.Features;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, require = 0)
    private int vanillahud$outlineColor(int color) {
        if (!Features.BlockOutline.active()) return color;
        return Features.BlockOutline.INSTANCE.color.get();
    }

    @ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, require = 0)
    private float vanillahud$outlineWidth(float width) {
        if (!Features.BlockOutline.active()) return width;
        return Features.BlockOutline.INSTANCE.width.f();
    }
}
