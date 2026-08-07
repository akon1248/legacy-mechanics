package com.akon.legacymechanics.item

import com.akon.legacymechanics.network.OutgoingItemStackEvent
import com.akon.fuel.loader.api.events.FuelEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents
import net.minecraft.world.item.enchantment.Enchantments

/**
 * Fixes MC-271840 in the outgoing attribute-modifiers view.
 *
 * Sharpness applies its damage bonus procedurally via [EnchantmentEffectComponents.DAMAGE], never
 * as an attribute modifier, so a weapon's mainhand ATTACK_DAMAGE modifier amount never reflects
 * it -- the client's tooltip renders straight from that amount, whatever [ItemAttributeModifiers.Display]
 * the entry carries, so the enchantment bonus is simply missing from the number shown.
 *
 * Fix: fold Sharpness's live bonus into the outgoing modifier's amount and leave everything else
 * (id, operation, display type) untouched. This project's own weapons already carry their
 * ATTACK_DAMAGE modifier under a non-vanilla id specifically to get the ordinary additive
 * "+X Attack Damage" rendering rather than
 * vanilla's calculated-total display -- that rendering reads directly off the amount, so bumping
 * it is all that is needed; no lore rewriting or tooltip-section hiding required.
 */
object SharpnessTooltip {

    // Enchantments are registry entries, not static objects, so resolving the Holder needs a
    // live registry access -- safe to cache: the enchantment registry (unlike a mutable tag) is frozen after
    // boot and never rewritten at runtime.
    private val sharpness by lazy {
        MinecraftServer.getServer().registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.SHARPNESS)
    }

    fun register() {
        FuelEvents.BUS.addListener<OutgoingItemStackEvent> { render(it) }
    }

    private fun render(event: OutgoingItemStackEvent) {
        val source = event.stack
        val modifiers = source.get(DataComponents.ATTRIBUTE_MODIFIERS) ?: return
        val entries = modifiers.modifiers()
        val index = entries.indexOfFirst { it.attribute == Attributes.ATTACK_DAMAGE && it.slot == EquipmentSlotGroup.MAINHAND && it.modifier.operation == AttributeModifier.Operation.ADD_VALUE }
        if (index < 0) return

        val level = source.get(DataComponents.ENCHANTMENTS)?.getLevel(sharpness) ?: 0
        if (level <= 0) return

        val bonus = 1.25 * level
        val damageEntry = entries[index]
        val boosted = ItemAttributeModifiers.Entry(
            damageEntry.attribute,
            AttributeModifier(damageEntry.modifier.id(), damageEntry.modifier.amount() + bonus, damageEntry.modifier.operation()),
            damageEntry.slot,
            damageEntry.display,
        )
        val rebuilt = entries.toMutableList().also { it[index] = boosted }
        event.editCopy().set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers(rebuilt))
    }
}
