package com.akon.legacymechanics.network

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.TranslatableContents

/**
 * Counterpart to [OutgoingItemStacks] for items that never reach the wire as an `ItemStack`
 * field: a `show_item` hover event serializes the stack through `ItemStack.MAP_CODEC` into NBT
 * nested inside the `Component`, not through a `StreamCodec`, so [OutgoingItemStacks.transform]
 * never sees it. Called from the encoder mixin on the same connection thread that binds
 * [OutgoingItemStacks]'s recipient, so [OutgoingItemStacks.transform] already knows who it's
 * encoding for.
 *
 * Walks [Component.getStyle], [Component.getSiblings], and -- because a translatable's format
 * args are neither -- [TranslatableContents]'s args. That last one is not an edge case: `/give`'s
 * feedback message is `Component.translatable("commands.give.success.single", count,
 * stack.getDisplayName(), target)`, and the item's hover-linked name lives in that arg list, not
 * as a sibling. A hover event can itself carry a `Component` (`show_text`'s value,
 * `show_entity`'s name) but those are tooltips shown over other tooltips -- not a path a normal
 * chat/sign/book/command message produces -- so they are left alone rather than recursed into.
 */
object OutgoingComponents {

    @JvmStatic
    fun transform(component: Component): Component {
        val hover = component.style.hoverEvent
        val newStyle = if (hover is HoverEvent.ShowItem) {
            val transformed = OutgoingItemStacks.transform(hover.item())
            if (transformed === hover.item()) null else component.style.withHoverEvent(HoverEvent.ShowItem(transformed))
        } else {
            null
        }

        val contents = component.contents
        val newContents = if (contents is TranslatableContents) {
            val args = contents.args
            var changed = false
            val newArgs = arrayOfNulls<Any?>(args.size)
            for (i in args.indices) {
                val arg = args[i]
                if (arg is Component) {
                    val transformedArg = transform(arg)
                    if (transformedArg !== arg) changed = true
                    newArgs[i] = transformedArg
                } else {
                    newArgs[i] = arg
                }
            }
            @Suppress("UNCHECKED_CAST")
            if (changed) TranslatableContents(contents.key, contents.fallback, newArgs as Array<out Any>) else null
        } else {
            null
        }

        val siblings = component.siblings
        var newSiblings: MutableList<Component>? = null
        for (i in siblings.indices) {
            val sibling = siblings[i]
            val transformedSibling = transform(sibling)
            if (transformedSibling !== sibling) {
                val copy = newSiblings ?: ArrayList(siblings).also { newSiblings = it }
                copy[i] = transformedSibling
            }
        }

        if (newStyle == null && newContents == null && newSiblings == null) return component

        val rebuilt = MutableComponent.create(newContents ?: contents)
        rebuilt.style = newStyle ?: component.style
        for (sibling in newSiblings ?: siblings) {
            rebuilt.append(sibling)
        }
        return rebuilt
    }
}
