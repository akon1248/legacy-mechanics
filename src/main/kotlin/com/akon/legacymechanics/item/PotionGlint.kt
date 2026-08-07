package com.akon.legacymechanics.item

import com.akon.legacymechanics.network.OutgoingItemStackEvent
import com.akon.fuel.loader.api.events.FuelEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.PotionItem

/**
 * Forces the outgoing enchantment glint on for potion-type stacks that actually carry effects.
 *
 * ENCHANTMENT_GLINT_OVERRIDE is a per-stack conditional, not a per-item-type default, so it's set
 * here rather than through DefaultComponentOverrides. A plain water bottle (no effects) is left
 * untouched -- it already doesn't glint by default -- so only the true case is ever written.
 */
object PotionGlint {
    fun register() {
        FuelEvents.BUS.addListener<OutgoingItemStackEvent> { glint(it) }
    }

    private fun glint(event: OutgoingItemStackEvent) {
        val source = event.stack
        if (source.item !is PotionItem) return
        val contents = source.get(DataComponents.POTION_CONTENTS) ?: return
        if (!contents.hasEffects()) return

        event.editCopy().set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
    }
}
