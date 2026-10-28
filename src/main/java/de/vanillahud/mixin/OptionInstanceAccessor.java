package de.vanillahud.mixin;

import net.minecraft.client.OptionInstance;

/** Implemented onto OptionInstance by OptionInstanceMixin; writes values the normal setter would reject. */
public interface OptionInstanceAccessor {
    void vanillahud$setValue(Object value);

    static void set(OptionInstance<?> option, Object value) {
        ((OptionInstanceAccessor) (Object) option).vanillahud$setValue(value);
    }
}
