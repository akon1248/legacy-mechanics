package com.akon.legacymechanics.item

import com.akon.legacymechanics.network.OutgoingItemStackEvent
import com.akon.fuel.loader.api.events.FuelEvents
import com.akon.legacymechanics.mixin.potioncolors.MobEffectAccessor
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.item.Items
import net.minecraft.world.item.PotionItem
import net.minecraft.world.item.alchemy.PotionContents
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * Re-blends the outgoing potion color using the legacy per-effect colors restored by mixin/potioncolors/.
 *
 * PotionContents.getColorOptional already computes the weighted-average blend across an effect
 * list -- the same algorithm the swirl-particle mixin reimplements separately for the entity data
 * channel -- so this only calls it and stamps the result on as a customColor. A stack that already
 * carries an explicit custom color is left untouched.
 */
object LegacyPotionColors {
    
    private val LEGACY_COLOR_MAP = mapOf(
        "speed" to 8171462,
        "slowness" to 5926017,
        "strength" to 9643043,
        "instant_damage" to 4393481,
        "jump_boost" to 2293580,
        "resistance" to 10044730,
        "fire_resistance" to 14981690,
        "water_breathing" to 3035801,
        "invisibility" to 8356754,
        "night_vision" to 2039713,
        "luck" to 3381504,
        "poison" to 5149489,
        "wither" to 3484199,
        "slow_falling" to 16773073
    )

    private val IDS_TO_RECOLOR = LEGACY_COLOR_MAP.keys.map(Identifier::withDefaultNamespace)

    @JvmStatic
    fun recolorEffect(id: String, effect: MobEffect) {
        LEGACY_COLOR_MAP[id]?.let { (effect as MobEffectAccessor).setColor(it) }
    }
    
    fun register() {
        FuelEvents.BUS.addListener<OutgoingItemStackEvent> { recolorPotion(it) }
    }

    private fun recolorPotion(event: OutgoingItemStackEvent) {
        val source = event.stack
        if (source.item !is PotionItem && !source.`is`(Items.TIPPED_ARROW)) return
        val contents = source.get(DataComponents.POTION_CONTENTS) ?: return
        if (contents.customColor().isPresent) return
        if (!contents.hasEffects()) return

        val effects = contents.allEffects
        if (effects.mapNotNull { it.effect.unwrapKey().getOrNull() }.none { IDS_TO_RECOLOR.contains(it.identifier()) })
            return
        val computedColor = PotionContents.getColorOptional(effects)
        if (!computedColor.isPresent) return

        val updated = PotionContents(contents.potion(), Optional.of(computedColor.asInt), contents.customEffects(), contents.customName())
        event.editCopy().set(DataComponents.POTION_CONTENTS, updated)
    }
}
