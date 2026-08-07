package com.akon.legacymechanics.freeingredients

import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/** A menu slot whose server-provided ingredient cannot be moved or replaced by players. */
class LockedIngredientSlot @JvmOverloads constructor(
    container: Container,
    slot: Int,
    x: Int,
    y: Int,
    private val displayedStack: ItemStack? = null,
) : Slot(container, slot, x, y) {

    override fun getItem(): ItemStack = displayedStack ?: super.getItem()

    override fun mayPlace(stack: ItemStack): Boolean = false

    override fun mayPickup(player: Player): Boolean = false
}
