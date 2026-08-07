package com.akon.legacymechanics.mixin.itemview;

import com.akon.legacymechanics.network.OutgoingItemStackPayloads;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Prevents a creative client from turning a spoofed outgoing item view into authoritative server
 * state by restoring the original component patch immediately before the creative packet's stack
 * is accepted.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @WrapOperation(
        method = "handleSetCreativeModeSlot(Lnet/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;setByPlayer(Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void restoreOutgoingViewBeforeCreativeSet(
        Slot slot,
        ItemStack stack,
        Operation<Void> original
    ) {
        original.call(slot, OutgoingItemStackPayloads.restore(this.player, stack));
    }

    @WrapOperation(
        method = "handleSetCreativeModeSlot(Lnet/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/InventoryMenu;setRemoteSlot(ILnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void restoreOutgoingViewBeforeCreativeRemoteSet(
        InventoryMenu menu,
        int slot,
        ItemStack stack,
        Operation<Void> original
    ) {
        original.call(menu, slot, OutgoingItemStackPayloads.restore(this.player, stack));
    }

    @WrapOperation(
        method = "handleSetCreativeModeSlot(Lnet/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    private ItemEntity restoreOutgoingViewBeforeCreativeDrop(
        ServerPlayer player,
        ItemStack stack,
        boolean throwRandomly,
        Operation<ItemEntity> original
    ) {
        return original.call(player, OutgoingItemStackPayloads.restore(this.player, stack), throwRandomly);
    }
}
