package com.akon.legacymechanics.mixin.itemview;

import com.akon.legacymechanics.network.OutgoingComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Lets a player be shown a different item than the server holds inside a chat/sign/book hover
 * tooltip, the same way {@link ItemStackOptionalStreamCodecMixin} does for inventory packets.
 *
 * <p>The class name does not follow {@code <Target>Mixin} for the same reason as that sibling
 * mixin: the target is the anonymous class {@code ComponentSerialization$1}, produced by
 * {@code ComponentSerialization.createTranslationAware}, which has no legal Java name to mirror.
 *
 * <p>This is the single funnel for every outgoing {@code Component} -- chat, signs, books, boss
 * bars, tab list, disconnect screens -- because both {@code STREAM_CODEC} and
 * {@code TRUSTED_STREAM_CODEC} are instances of this one anonymous class. A {@code show_item}
 * hover event's `ItemStack` is nested inside the NBT this codec writes, so rewriting the
 * component here is what {@link OutgoingComponents#transform} is for.
 */
@Mixin(targets = "net.minecraft.network.chat.ComponentSerialization$1")
public abstract class ComponentSerializationStreamCodecMixin {

    /**
     * The full descriptor is load-bearing beyond the usual overload risk: erasure gives this
     * class a synthetic {@code encode(Object, Object)} bridge, and a bare name would match both.
     */
    @ModifyVariable(
        method = "encode(Lnet/minecraft/network/RegistryFriendlyByteBuf;Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component rewriteShowItemHovers(Component component) {
        return OutgoingComponents.transform(component);
    }
}
