package de.vanillahud.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets us write option values that the normal setter would reject (e.g. gamma above 1.0). */
@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor {
    @Accessor("value")
    void vanillahud$setValue(Object value);

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void set(OptionInstance<?> option, Object value) {
        ((OptionInstanceAccessor) (Object) option).vanillahud$setValue(value);
    }
}
