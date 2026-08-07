package com.akon.legacymechanics.item

import com.akon.legacymechanics.network.OutgoingItemStackEvent
import com.akon.fuel.loader.api.events.FuelEvents
import com.akon.legacymechanics.network.OutgoingItemStacks
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Items
import net.minecraft.world.item.PotionItem
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.TooltipDisplay

/**
 * Re-renders legacy Strength/Weakness potion information in the outgoing lore view.
 *
 * PotionContents is hidden because its vanilla section would otherwise be duplicated. The
 * built-in renderer supplies the replacement lines first, followed by the stack's existing lore.
 */
object LegacyPotionTooltips {
    fun register() {
        FuelEvents.BUS.addListener<OutgoingItemStackEvent> { render(it) }
    }

    private fun render(event: OutgoingItemStackEvent) {
        val source = event.stack
        if (source.item !is PotionItem && !source.`is`(Items.TIPPED_ARROW)) return
        val contents = source.get(DataComponents.POTION_CONTENTS) ?: return
        if (contents.allEffects.none { it.effect == MobEffects.STRENGTH || it.effect == MobEffects.WEAKNESS }) return

        val view = event.editCopy()
        view.set(
            DataComponents.TOOLTIP_DISPLAY,
            view.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).withHidden(DataComponents.POTION_CONTENTS, true),
        )

        val potionLines = mutableListOf<Component>()
        PotionContents.addPotionTooltip(
            contents.allEffects,
            potionLines::add,
            view.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F),
            event.recipient.level().tickRateManager().tickrate(),
        )
        // ItemLore serializes only raw lines; its rendered-lines constructor parameter is lost
        // across the network. An explicit false survives serialization and wins when the client
        // merges ItemLore's default italic style.
        val existingLore = view.getOrDefault(DataComponents.LORE, ItemLore.EMPTY)
        val nonItalicPotionLines = potionLines.map { line -> line.copy().withStyle { style -> style.withItalic(false) } }
        view.set(DataComponents.LORE, ItemLore(nonItalicPotionLines + existingLore.lines()))
    }
}
