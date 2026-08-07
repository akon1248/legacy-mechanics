package com.akon.legacymechanics.mixin.potioncolors;

import com.akon.legacymechanics.item.LegacyPotionColors;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restores the pre-1.19.4 / pre-23w12a decimal RGB {@code color} for 14 {@code MobEffect} registry entries.
 *
 * <p>Every one of {@code MobEffects}' 41 registrations funnels through this single private
 * {@code register} helper (MobEffects.java:129-131) before the effect reaches
 * {@code BuiltInRegistries.MOB_EFFECT} via {@code Registry.registerForHolder}. Injecting at
 * {@code HEAD} -- before that call runs -- and overwriting the field there via
 * {@link MobEffectAccessor} (the field is private final, so this is the only way to write it)
 * guarantees every downstream consumer registered from this point on, including the potion swirl
 * mixin (mixin/potionparticles/LivingEntityMixin.java) and
 * {@code PotionContents.getColorOptional}, sees the restored value.
 *
 * <p>Effects not in the table below (e.g. haste, mining_fatigue, regeneration) are left
 * untouched -- their color never changed across the 1.19.4 / 23w12a shift.
 */
@Mixin(MobEffects.class)
public abstract class MobEffectsMixin {

    @Inject(
        method = "register(Ljava/lang/String;Lnet/minecraft/world/effect/MobEffect;)Lnet/minecraft/core/Holder;",
        at = @At("HEAD")
    )
    private static void legacy_mechanics$restoreLegacyColor(String name, MobEffect effect, CallbackInfoReturnable<Holder<MobEffect>> cir) {
        LegacyPotionColors.recolorEffect(name, effect);
    }
}
