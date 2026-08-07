package com.akon.legacymechanics.mixin.potioncolors;

import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes a setter for {@code MobEffect}'s private final {@code color} field (MobEffect.java:45),
 * assigned once in the constructor and never again. {@link MobEffectsMixin} uses this to overwrite
 * the field for 14 legacy-changed effects at registration time, before anything can have read the
 * post-1.19.4 value.
 */
@Mixin(MobEffect.class)
public interface MobEffectAccessor {

    @Mutable
    @Accessor("color")
    void setColor(int color);
}
