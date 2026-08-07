package com.akon.legacymechanics.network

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.Event

/**
 * Fired on [com.akon.fuel.loader.api.events.FuelEvents.BUS] for every ItemStack about to be
 * written to [recipient]'s connection, letting a listener show that one player something
 * different from what the server actually holds -- per-player names, hidden enchantments,
 * team-coloured gear.
 *
 * Nothing is persisted: the replacement is serialized and discarded, so the server-side stack,
 * the inventory and the save file are untouched.
 *
 * Two things a listener must know:
 *
 * 1. **This runs on the connection's netty thread, not the main thread.** Touching world or
 *    entity state from here is a data race. Read what you need off [recipient] only if it is
 *    safe to read concurrently, or better, precompute it on the main thread and look it up here.
 * 2. **This is a very hot path** -- once per stack per packet, so every inventory sync,
 *    equipment update and entity-data broadcast. Keep listeners allocation-free and branch out
 *    early; do not do lookups that could be cached.
 *
 * Never mutate [original] -- it is the server's live stack, shared with every other recipient
 * and with the inventory itself. Call [editCopy] and mutate that, or assign [stack] outright.
 */
class OutgoingItemStackEvent(
    @JvmField val recipient: ServerPlayer,
    @JvmField val original: ItemStack,
) : Event() {

    /**
     * What will actually be encoded. Defaults to [original]; assign to replace it wholesale.
     */
    @JvmField
    var stack: ItemStack = original

    private var copied = false

    /**
     * Returns a copy of [stack] that is safe to mutate, having already installed it as [stack].
     *
     * Idempotent within one event: several listeners can each call this and they all edit the
     * same copy rather than each cloning the previous one.
     */
    fun editCopy(): ItemStack {
        if (!copied) {
            stack = stack.copy()
            copied = true
        }
        return stack
    }
}
