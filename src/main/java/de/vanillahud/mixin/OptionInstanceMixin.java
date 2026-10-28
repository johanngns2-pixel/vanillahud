package de.vanillahud.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OptionInstance.class)
public abstract class OptionInstanceMixin implements OptionInstanceAccessor {
    @Shadow
    @Mutable
    Object value;

    @Override
    public void vanillahud$setValue(Object v) {
        this.value = v;
    }
}
