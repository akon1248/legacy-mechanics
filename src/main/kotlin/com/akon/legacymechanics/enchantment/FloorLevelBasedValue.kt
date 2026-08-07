package com.akon.legacymechanics.enchantment

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.Mth
import net.minecraft.world.item.enchantment.LevelBasedValue

/**
 * `floor(value)` as a [LevelBasedValue] combinator -- the one Mojang left out. Real 1.8.8's
 * Enchantment Protection Factor formula is `floor((6 + level^2) * TypeModifier / 3)`; every combinator vanilla
 * ships is pure float math, so without this, the protection-family jsons had to pre-bake four
 * floored values into a `lookup` table and fall back to an unfloored approximation for levels
 * 5+. Wrapping the existing fraction in this instead lets every level use the exact formula.
 *
 * Registered into `LevelBasedValue`'s dispatch registry by
 * [com.akon.legacymechanics.mixin.protection.LevelBasedValueMixin] -- not networkable on its own,
 * see [[synced-enchantment-registry-crash-risk]] for why
 * [com.akon.legacymechanics.network.OutgoingEnchantmentEffects] has to strip this type out of what
 * a client without this mixin receives.
 */
data class FloorLevelBasedValue(val value: LevelBasedValue) : LevelBasedValue {

    override fun calculate(level: Int): Float = Mth.floor(value.calculate(level)).toFloat()

    override fun codec(): MapCodec<FloorLevelBasedValue> = CODEC

    companion object {
        @JvmField
        val CODEC: MapCodec<FloorLevelBasedValue> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                LevelBasedValue.CODEC.fieldOf("value").forGetter(FloorLevelBasedValue::value)
            ).apply(instance, ::FloorLevelBasedValue)
        }
    }
}
