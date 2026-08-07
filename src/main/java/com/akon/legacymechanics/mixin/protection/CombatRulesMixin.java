package com.akon.legacymechanics.mixin.protection;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.damagesource.CombatRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restores the pre-1.9 randomized Enchantment Protection Factor (EPF) roll, matching the exact
 * integer formula from decompiled 1.8.8 {@code EnchantmentHelper.getEnchantmentModifierDamage}:
 * the summed EPF is capped at 25, then the *effective* EPF is a uniformly-random integer in
 * {@code [ceil(sum/2), sum]} -- not a continuous 0.5-1.0 multiplier rounded up, which looked
 * equivalent but has a different (non-uniform) distribution over the achievable integers. That
 * result is capped again at 20, replacing the modern deterministic {@code clamp(sum, 0, 20)}.
 *
 * <p>{@code enchantModifiers} arrives here as the raw, uncapped per-hit EPF sum -- summing every
 * piece's contribution is the one step of the old pipeline the modern engine still performs
 * upstream (in {@code EnchantmentHelper.getDamageProtection}), so this mixin only needs to
 * replace what happens to that sum, not redo the summation itself.
 */
@Mixin(CombatRules.class)
public abstract class CombatRulesMixin {

    /** Old Java clamps the raw EPF sum to this before it is ever rolled. */
    @Unique
    private static final int RAW_EPF_CAP = 25;

    /** Old Java's final usable-EPF ceiling, same value the modern clamp also used. */
    @Unique
    private static final int EFFECTIVE_EPF_CAP = 20;

    /** 1 EPF point is a 4% reduction, i.e. reduction = effectiveEpf / 25. */
    @Unique
    private static final float EPF_TO_DAMAGE_DIVISOR = 25.0F;

    @Inject(
        method = "getDamageAfterMagicAbsorb(FF)F",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void rollLegacyRandomizedEpf(
        float damageAmount,
        float enchantModifiers,
        CallbackInfoReturnable<Float> cir
    ) {
        int cappedSum = Math.min((int) enchantModifiers, RAW_EPF_CAP);
        int minRoll = (cappedSum + 1) >> 1;
        int rollSpread = (cappedSum >> 1) + 1;
        int effectiveEpf = Math.min(minRoll + ThreadLocalRandom.current().nextInt(rollSpread), EFFECTIVE_EPF_CAP);
        cir.setReturnValue(damageAmount * (1.0F - effectiveEpf / EPF_TO_DAMAGE_DIVISOR));
    }
}
