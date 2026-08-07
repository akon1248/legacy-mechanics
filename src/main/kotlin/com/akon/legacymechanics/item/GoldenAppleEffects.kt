package com.akon.legacymechanics.item

import net.minecraft.core.component.DataComponents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.Consumable
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect

/**
 * Enchanted golden apple effects, tuned rather than a strict pre-1.9 restoration: Regeneration V
 * for 30 seconds, Resistance I and Fire Resistance both for 5 minutes (unchanged from vanilla),
 * and Absorption I for 2 minutes.
 *
 * Vanilla's current `Consumables.ENCHANTED_GOLDEN_APPLE` grants Regeneration II (20s), Resistance
 * I (5 min), Fire Resistance (5 min), and Absorption IV (2 min). Only Regeneration and Absorption
 * differ here: Regeneration is boosted well past vanilla's value, and Absorption is brought back
 * down to I -- the pre-1.9 amplifier, before the 1.9 buff to IV.
 */
object GoldenAppleEffects {

    private val NOTCH_APPLE_CONSUMABLE: Consumable = Consumable.builder()
        .onConsume(
            ApplyStatusEffectsConsumeEffect(
                listOf(
                    MobEffectInstance(MobEffects.REGENERATION, 600, 4),
                    MobEffectInstance(MobEffects.RESISTANCE, 6000, 0),
                    MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0),
                    MobEffectInstance(MobEffects.ABSORPTION, 2400, 0),
                ),
            ),
        )
        .build()

    fun register() {
        DefaultComponentOverrides.override(
            Items.ENCHANTED_GOLDEN_APPLE,
            DataComponents.CONSUMABLE,
            NOTCH_APPLE_CONSUMABLE,
        )
    }
}
