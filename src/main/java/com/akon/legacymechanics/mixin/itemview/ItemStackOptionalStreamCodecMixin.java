package com.akon.legacymechanics.mixin.itemview;

import com.akon.legacymechanics.network.OutgoingItemStacks;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Lets a player be shown different item data than the server holds -- custom names, hidden
 * enchantments, team-coloured gear -- by rewriting each stack as it is written to that
 * player's connection. The server-side stack is never touched.
 *
 * <p>Two deliberate deviations from the project's mixin conventions, both forced by the target:
 *
 * <p><b>The class name does not follow {@code <Target>Mixin}.</b> The target is the anonymous
 * class {@code ItemStack$2}, which has no legal Java name to mirror, so it is named for what
 * that codec <i>is</i> ({@code ItemStack.createOptionalStreamCodec}'s product) instead.
 *
 * <p><b>Plain {@code @ModifyVariable} rather than MixinExtras.</b> The thing being replaced is
 * an incoming parameter, and MixinExtras has no equivalent for that; every subsequent read of
 * {@code value} in the target must see the replacement, which modifying the local slot at HEAD
 * achieves and a value-returning injector would not.
 *
 * <p>This is the single funnel for stacks serialized <i>as ItemStack fields</i>: every
 * clientbound item packet, plus stacks nested in bundle/container contents, since those
 * components' stream codecs are built on this one. It does <b>not</b> catch items embedded in
 * text components -- a {@code show_item} hover serializes through {@code ItemStack.MAP_CODEC}
 * into NBT and never enters a stream codec at all.
 */
@Mixin(targets = "net.minecraft.world.item.ItemStack$2")
public abstract class ItemStackOptionalStreamCodecMixin {

    /**
     * The full descriptor is load-bearing beyond the usual overload risk: erasure gives this
     * class a synthetic {@code encode(Object, Object)} bridge, and a bare name would match both.
     */
    @ModifyVariable(
            method = "encode(Lnet/minecraft/network/RegistryFriendlyByteBuf;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            argsOnly = true,
            name = "value")
    private ItemStack applyRecipientItemView(ItemStack stack) {
        return OutgoingItemStacks.transform(stack);
    }
}
