package de.vanillahud.mixin;

import de.vanillahud.module.Modules;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    /** Der Server schickt alle 20 Ticks die Zeit -> daraus laesst sich die TPS schaetzen. */
    @Inject(method = "setTimeFromServer", at = @At("HEAD"), require = 0)
    private void vanillahud$time(long gameTime, long dayTime, boolean tick, CallbackInfo ci) {
        Modules.onTimePacket();
    }
}
