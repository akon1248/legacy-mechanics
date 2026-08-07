package com.akon.legacymechanics.mixin.potionparticles;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

/**
 * Restores the pre-1.19.4 potion swirl: one blended particle color averaged across all active
 * effects, instead of the modern per-effect color list the client randomly flickers between.
 *
 * 1.19.4 replaced {@code DATA_EFFECT_COLOR_ID} (a single synced blended {@code int}, computed by
 * the now-removed {@code PotionUtils.mixColor}) with {@code DATA_EFFECT_PARTICLES}, a synced
 * {@code List<ParticleOptions>} of each effect's own {@code ParticleOptions}. The old wire format
 * (color smuggled through a particle packet's velocity fields) no longer exists client-side, so
 * the fix can't just resurrect the old field -- it has to reproduce the old blend server-side and
 * hand the client a single-element list built from the current {@code ColorParticleOption}-based
 * {@code ENTITY_EFFECT} particle type instead.
 *
 * This only overwrites the entity swirl/ambient particle color. Splash/lingering impact
 * particles, tipped-arrow color, and the item-tooltip color average are separate mechanisms and
 * are untouched.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Final
    @Shadow
    private static EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES;
    @Shadow
    @Final
    private static EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID;

    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow
    public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> effects) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    public abstract Collection<MobEffectInstance> getActiveEffects();

    /**
     * {@code ParticleTypes.AMBIENT_ENTITY_EFFECT} no longer exists as a distinct particle type in
     * this version -- {@code ParticleTypes.ENTITY_EFFECT} is the only entity-effect particle type
     * left, declared {@code ParticleType<ColorParticleOption>}, and vanilla's own default
     * {@code MobEffect} particle factory now signals ambient-ness by dimming that
     * {@code ColorParticleOption}'s alpha channel rather than by picking a different type
     * (confirmed by reading {@code MobEffect}'s constructor and
     * {@code ParticleTypes.ENTITY_EFFECT}'s declaration in extracted_sources). This mixin always
     * emits full alpha and leaves {@code DATA_EFFECT_AMBIENCE_ID} exactly as vanilla just set it
     * a line above this injection -- that boolean alone still drives the client's lower ambient
     * spawn chance, and per-particle alpha dimming is a separate visual nuance out of scope for a
     * color-blend restoration.
     */
    @Inject(method = "updateSynchronizedMobEffectParticles()V", at = @At("HEAD"), cancellable = true)
    private void legacy_mechanics$blendEffectParticleColor(CallbackInfo ci) {
        ci.cancel();

        // Some effects (e.g. Trial Omen, Raid Omen) are built with MobEffect's fixed-particle
        // constructor and never used the color swirl at all -- their `color` field is unrelated
        // to what they render. Only fold effects that actually produce a swirl ColorParticleOption
        // into the blend; every other effect's own particle option is preserved untouched so it
        // keeps appearing in the list exactly as vanilla would have shown it.
        ImmutableList.Builder<ParticleOptions> builder = ImmutableList.builder();
        float redSum = 0.0F;
        float greenSum = 0.0F;
        float blueSum = 0.0F;
        int weightSum = 0;
        for (MobEffectInstance effect : this.getActiveEffects()) {
            if (!effect.isVisible()) continue;
            ParticleOptions options = effect.getParticleOptions();
            if (options.getType() != ParticleTypes.ENTITY_EFFECT) {
                builder.add(options);
                continue;
            }
            int color = effect.getEffect().value().getColor();
            int weight = effect.getAmplifier() + 1;
            redSum += (color >> 16 & 0xFF) / 255.0F * weight;
            greenSum += (color >> 8 & 0xFF) / 255.0F * weight;
            blueSum += (color & 0xFF) / 255.0F * weight;
            weightSum += weight;
        }

        if (weightSum > 0) {
            int red = Math.round(redSum / weightSum * 255.0F);
            int green = Math.round(greenSum / weightSum * 255.0F);
            int blue = Math.round(blueSum / weightSum * 255.0F);
            builder.add(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(red, green, blue)));
        }

        this.entityData.set(DATA_EFFECT_PARTICLES, builder.build());
        this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(this.getActiveEffects()));
    }
}
