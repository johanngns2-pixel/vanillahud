package de.vanillahud.mixin;

import de.vanillahud.module.Features;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void vanillahud$filter(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz,
                                   CallbackInfoReturnable<Particle> cir) {
        if (Features.Particles.hidden(options)) cir.setReturnValue(null);
    }

    @Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void vanillahud$filterEmitter(Entity entity, ParticleOptions options, CallbackInfo ci) {
        if (Features.Particles.hidden(options)) ci.cancel();
    }

    @Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void vanillahud$filterEmitter2(Entity entity, ParticleOptions options, int life, CallbackInfo ci) {
        if (Features.Particles.hidden(options)) ci.cancel();
    }
}
