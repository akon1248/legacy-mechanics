package com.akon.legacymechanics.network

import com.akon.legacymechanics.item.DefaultComponentOverrides
import com.akon.fuel.loader.api.events.FuelEvents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

/**
 * Plumbing behind [OutgoingItemStackEvent]: carries the packet's recipient from the netty
 * encoder down into the ItemStack stream codec, which has no idea who it is encoding for.
 *
 * A ThreadLocal is the right tool and not a hack here -- it is the same mechanism Paper uses
 * for the identical problem, twice: `PacketEncoder.ADVENTURE_LOCALE` and the whole
 * `ItemObfuscationSession`. It is sound because `PacketEncoder` is a per-channel netty handler
 * and the bind/clear pair brackets exactly one `encode` call, so no other connection can
 * interleave on that thread.
 */
object OutgoingItemStacks {

    private val RECIPIENT = ThreadLocal<ServerPlayer?>()

    /** Called from the encoder mixin. [player] is null for connections with no player yet. */
    @JvmStatic
    fun bindRecipient(player: ServerPlayer?) {
        RECIPIENT.set(player)
    }

    @JvmStatic
    fun clearRecipient() {
        RECIPIENT.remove()
    }

    /**
     * Returns what should be encoded in place of [stack].
     *
     * Both early-outs matter for throughput: stream codecs also serialize to buffers that are
     * not going to any player, and the empty-stack case is by far the most common thing in an
     * inventory packet.
     *
     * Overridden default components are written in last, deliberately: a listener that set or
     * removed one of them has already put its value in the stack's patch, and
     * [DefaultComponentOverrides.forceOntoWire] skips anything the patch already carries. So the
     * per-player view wins over the changed default, and the changed default only fills in where
     * no one had an opinion.
     *
     * **[OutgoingItemStackPayloads.attach] must run before that forcing step, not after.** Its
     * early-out asks whether the stack changed via [ItemStack.matches], which compares
     * *patches* rather than resolved values (`PatchedDataComponentMap.equals`). Forcing a
     * default writes a value the stack already resolved to, so it changes the patch without
     * changing meaning -- which reads to `attach` as a spoofed stack and would stamp a
     * restoration payload onto every stack of every overridden item, on this hot path. Ordered
     * this way, `attach` sees only what listeners actually did.
     */
    @JvmStatic
    fun transform(stack: ItemStack): ItemStack {
        if (stack.isEmpty) return stack
        val recipient = RECIPIENT.get() ?: return stack
        val event = OutgoingItemStackEvent(recipient, stack)
        FuelEvents.BUS.post(event)
        val rendered = OutgoingItemStackPayloads.attach(recipient, stack, event.stack)
        return DefaultComponentOverrides.forceOntoWire(rendered)
    }
}
