package com.akon.legacymechanics.item

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemAttributeModifiers

/**
 * Restores the item attributes which changed after 1.8 without removing modern items or slots.
 *
 * This is deliberately one component transformation per item: composing the speed/toughness
 * removal and axe damage replacement keeps either edit from restoring the other's component.
 */
object LegacyItemAttributes {
    /**
     * A non-vanilla ID retains the standard raw-modifier tooltip instead of the special
     * calculated-total rendering reserved for the base weapon-damage modifier.
     */
    private val LEGACY_ATTACK_DAMAGE_ID = Identifier.fromNamespaceAndPath("annihilation", "legacy_attack_damage")

    /**
     * Pre-1.9 attack-damage modifiers. These add to the player's inherent base damage of
     * one, for actual axe hits of 4/5/6/7 from wood/gold through diamond.
     */
    private val AXE_DAMAGE_BONUS = mapOf(
        Items.WOODEN_AXE to 3.0,
        Items.GOLDEN_AXE to 3.0,
        Items.STONE_AXE to 4.0,
        Items.COPPER_AXE to 4.0,
        Items.IRON_AXE to 5.0,
        Items.DIAMOND_AXE to 6.0,
        Items.NETHERITE_AXE to 7.0,
    )

    fun register() {
        // Tags are unavailable at PostBootstrap. The registry itself is complete, however, and
        // this intentionally covers every vanilla item rather than only the old tool set.
        BuiltInRegistries.ITEM.forEach { item ->
            val original = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS) ?: return@forEach
            val updated = transform(item, original)
            if (updated != original) DefaultComponentOverrides.override(item, DataComponents.ATTRIBUTE_MODIFIERS, updated)
        }
    }

    private fun transform(item: Item, original: ItemAttributeModifiers): ItemAttributeModifiers {
        val builder = ItemAttributeModifiers.builder()
        var changed = false
        original.modifiers().forEach { entry ->
            if (entry.attribute() == Attributes.ATTACK_SPEED || entry.attribute() == Attributes.ARMOR_TOUGHNESS) {
                changed = true
                return@forEach
            }

            if (entry.attribute() == Attributes.ATTACK_DAMAGE && entry.slot() == EquipmentSlotGroup.MAINHAND) {
                // Axes retain their deliberately lower legacy values. Every other tool carrying
                // a main-hand attack-damage modifier receives the same +1 rule as a sword,
                // including tools added after the original vanilla tier list.
                val damageBonus = AXE_DAMAGE_BONUS[item] ?: (entry.modifier().amount() + 1.0)
                val modifier = AttributeModifier(LEGACY_ATTACK_DAMAGE_ID, damageBonus, AttributeModifier.Operation.ADD_VALUE)
                builder.add(entry.attribute(), modifier, entry.slot(), entry.display())
                changed = true
            } else {
                builder.add(entry.attribute(), entry.modifier(), entry.slot(), entry.display())
            }
        }
        return if (changed) builder.build() else original
    }
}
