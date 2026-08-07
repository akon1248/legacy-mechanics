package com.akon.legacymechanics.network

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

/**
 * Makes per-recipient item views survive a creative-mode round-trip without turning the fake
 * view into authoritative server state.
 *
 * The payload is intentionally not authenticated. Creative players are already trusted to forge
 * arbitrary items; this is only a restoration hint that lets the server recover the original
 * component patch when one of our spoofed stacks comes back through
 * `ServerboundSetCreativeModeSlotPacket`.
 */
object OutgoingItemStackPayloads {

    private const val PAYLOAD_KEY = "legacy_mechanics:item_view"
    private const val ORIGINAL_COMPONENTS_KEY = "original_components"

    /**
     * Writes the original stack's component patch into the rendered stack's CUSTOM_DATA so the
     * creative inbound path can restore it later.
     */
    @JvmStatic
    fun attach(recipient: ServerPlayer, original: ItemStack, rendered: ItemStack): ItemStack {
        if (rendered.isEmpty || ItemStack.matches(original, rendered)) return rendered

        val target = if (rendered === original) rendered.copy() else rendered
        stripPayload(target)

        val originalPatch = snapshotWithoutPayload(original).componentsPatch
        val encodedPatch = encodePatch(recipient, originalPatch) ?: return target

        CustomData.update(DataComponents.CUSTOM_DATA, target) { customData ->
            val payload = CompoundTag()
            payload.put(ORIGINAL_COMPONENTS_KEY, encodedPatch)
            customData.put(PAYLOAD_KEY, payload)
        }
        return target
    }

    /**
     * Restores the original component patch from a spoofed outgoing stack that a creative client
     * sent back to the server. Mutating the passed stack is intentional: at this point it is an
     * inbound packet-local object rather than shared inventory state.
     */
    @JvmStatic
    fun restore(player: ServerPlayer, stack: ItemStack): ItemStack {
        if (stack.isEmpty) return stack

        val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return stack
        val root = customData.copyTag()
        val payload = root.get(PAYLOAD_KEY) as? CompoundTag ?: return stack
        root.remove(PAYLOAD_KEY)
        CustomData.set(DataComponents.CUSTOM_DATA, stack, root)

        val patchTag = payload.get(ORIGINAL_COMPONENTS_KEY) ?: return stack
        val patch = decodePatch(player, patchTag) ?: return stack
        // applyComponentsAndValidate merges the patch and leaves components introduced only by
        // the outgoing view (lore and tooltip display) in place. Creative then sends that view
        // back a second time. Restoring the complete original patch replaces those components.
        (stack.components as PatchedDataComponentMap).restorePatch(patch)
        return stack
    }

    private fun snapshotWithoutPayload(stack: ItemStack): ItemStack =
        stack.copy().also(::stripPayload)

    private fun stripPayload(stack: ItemStack) {
        val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return
        val root = customData.copyTag()
        if (root.get(PAYLOAD_KEY) !is CompoundTag) return
        root.remove(PAYLOAD_KEY)
        CustomData.set(DataComponents.CUSTOM_DATA, stack, root)
    }

    private fun encodePatch(player: ServerPlayer, patch: DataComponentPatch): Tag? =
        DataComponentPatch.CODEC.encodeStart(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), patch)
            .result()
            .orElse(null)

    private fun decodePatch(player: ServerPlayer, tag: Tag): DataComponentPatch? =
        DataComponentPatch.CODEC.parse(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag)
            .result()
            .orElse(null)
}
