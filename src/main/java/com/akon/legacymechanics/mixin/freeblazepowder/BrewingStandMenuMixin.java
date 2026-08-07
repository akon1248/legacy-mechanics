package com.akon.legacymechanics.mixin.freeblazepowder;

import com.akon.legacymechanics.freeingredients.LockedIngredientSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives every brewing stand a permanently stocked, player-locked blaze-powder slot. */
@Mixin(BrewingStandMenu.class)
public abstract class BrewingStandMenuMixin {

    @Unique
    private static final int FREE_BLAZE_POWDER_COUNT = 99;

    @ModifyArg(
        method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4)
    )
    private Slot lockFuelSlot(Slot original) {
        ItemStack blazePowder = new ItemStack(Items.BLAZE_POWDER, FREE_BLAZE_POWDER_COUNT);
        blazePowder.set(DataComponents.MAX_STACK_SIZE, FREE_BLAZE_POWDER_COUNT);
        return new LockedIngredientSlot(
            original.container, original.slot, original.x, original.y, blazePowder

        );
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V", at = @At("RETURN"))
    private void stockFreeBlazePowder(int containerId, Inventory inventory, Container brewingStand,
                                      ContainerData brewingStandData, CallbackInfo ci) {
        this.legacy_mechanics$freeIngredientSlot(4).set(ItemStack.EMPTY);
    }

    @Inject(method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void keepBlazePowderInItsSlot(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex == 4) cir.setReturnValue(ItemStack.EMPTY);
    }

    @Unique
    private Slot legacy_mechanics$freeIngredientSlot(int slotId) {
        return ((AbstractContainerMenu) (Object) this).getSlot(slotId);
    }
}
