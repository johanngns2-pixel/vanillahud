package de.vanillahud.mixin;

import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FogRenderer.class)
public interface FogRendererAccessor {
    @Accessor("fogEnabled")
    static void vanillahud$setFogEnabled(boolean v) {
        throw new AssertionError();
    }
}
