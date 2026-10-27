package de.vanillahud.mixin;

import de.vanillahud.module.Tps;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleSetTime", at = @At("HEAD"), require = 0)
    private void vanillahud$time(ClientboundSetTimePacket packet, CallbackInfo ci) {
        Tps.onTime(packet.gameTime());
    }

    @Inject(method = "handlePongResponse", at = @At("HEAD"), require = 0)
    private void vanillahud$pong(ClientboundPongResponsePacket packet, CallbackInfo ci) {
        de.vanillahud.module.PingTracker.onPong(packet.time());
    }
}
