package de.vanillahud.mixin;

import de.vanillahud.module.Features;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
    private void vanillahud$ping(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        Features.NametagPing m = Features.NametagPing.INSTANCE;
        if (m == null || !m.enabled || state.nameTag == null || !(entity instanceof Player p)) return;
        Minecraft mc = Minecraft.getInstance();
        if (p == mc.player && !m.self.value) return;
        if (mc.getConnection() == null) return;
        PlayerInfo info = mc.getConnection().getPlayerInfo(p.getUUID());
        if (info == null) return;
        state.nameTag = m.decorate(state.nameTag, info.getLatency());
    }
}
