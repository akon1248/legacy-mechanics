package com.akon.legacymechanics.item

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.component.UseCooldown

/**
 * Zeroes every item's USE_COOLDOWN component -- the fixed post-use delay Ender Pearl (1.0s),
 * Chorus Fruit (1.0s), and Wind Charge (0.5s) each carry by default. No such mechanic existed in
 * 1.8; Ender Pearl and Chorus Fruit could be used every tick, and Wind Charge postdates 1.8
 * entirely.
 *
 * Applies to the whole item registry rather than a hardcoded list, matching
 * [LegacyItemAttributes]: any future item that ships with a use cooldown is covered without an
 * update here. Zeroing rather than removing the component keeps every reader that expects
 * `Item.getUseCooldown` (or the component itself) to resolve consistent -- the cooldown system
 * still runs, it just clears in the same tick it was applied.
 */
object NoItemCooldowns {

    private val NO_COOLDOWN = UseCooldown(0.0F)

    fun register() {
        BuiltInRegistries.ITEM.forEach { item ->
            if (item.components().has(DataComponents.USE_COOLDOWN)) {
                DefaultComponentOverrides.override(item, DataComponents.USE_COOLDOWN, NO_COOLDOWN)
            }
        }
    }
}
