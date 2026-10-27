package de.vanillahud.mixin;

import de.vanillahud.module.Modules;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Partikel-Filter: einzelne Partikel-Arten komplett unterdruecken. */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void vanillahud$create(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz,
                                   CallbackInfoReturnable<Particle> cir) {
        if (Modules.particleHidden(options)) cir.setReturnValue(null);
    }

    @Inject(method = "destroy(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void vanillahud$destroy(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (Modules.particleHidden("block")) ci.cancel();
    }

    @Inject(method = "crack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void vanillahud$crack(BlockPos pos, Direction side, CallbackInfo ci) {
        if (Modules.particleHidden("block")) ci.cancel();
    }
}
