package com.akon.legacymechanics.enchantment

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.EnchantmentKeys
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys

/**
 * Pulls Sweeping Edge out of `#minecraft:non_treasure` at the post-flatten tag stage, i.e. after
 * vanilla's own tag contents are merged in but before they're frozen -- rather than a static
 * datapack tag file re-listing the other 35 entries by hand, which would silently go stale if a
 * future Minecraft version changes what's in that tag. `non_treasure` is nested inside
 * `in_enchanting_table`, `on_random_loot`, `tradeable`, `on_traded_equipment`, and
 * `on_mob_spawn_equipment`, so this alone removes Sweeping Edge from every vanilla source of
 * random acquisition (enchanting table, loot, villager trades, traded/mob equipment). It doesn't
 * need to run again on `/reload` -- `TAGS.postFlatten` is a [io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent],
 * fired fresh from the merged tag map on every reload, not just server start.
 *
 * Paired with `data/minecraft/enchantment/sweeping_edge.json`, which empties the enchantment's
 * own `supported_items` so it can never be applied to a weapon even if a book somehow exists.
 */
object DisabledEnchantmentTags {

    fun register(context: BootstrapContext) {
        context.lifecycleManager.registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT)) { event ->
            val registrar = event.registrar()
            if (!registrar.hasTag(EnchantmentTagKeys.NON_TREASURE)) return@registerEventHandler
            val current = registrar.getTag(EnchantmentTagKeys.NON_TREASURE)
            registrar.setTag(EnchantmentTagKeys.NON_TREASURE, current.filterNot { it == EnchantmentKeys.SWEEPING_EDGE })
        }
    }
}
