package com.akon.legacymechanics.mixin.enchanting;

import com.akon.legacymechanics.freeingredients.LockedIngredientSlot;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores the pre-1.8 table: enchantment power is the XP cost, with no lapis or clue. */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    /** The maximum pre-1.8 enchantment power, and therefore the fake modern lapis stack size. */
    @Unique
    private static final int MAX_ENCHANTMENT_POWER = 30;

    /** Keeps the modern client-side screen's lapis gate open without making lapis a cost. */
    @Unique
    private static final int FREE_LAPIS_COUNT = 99;

    @Final @Shadow public int[] enchantClue;
    @Final @Shadow public int[] levelClue;

    @Unique private int[] legacy_mechanics$hiddenEnchantClue = new int[0];
    @Unique private int[] legacy_mechanics$hiddenLevelClue = new int[0];

    @ModifyArg(
        method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/EnchantmentMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1)
    )
    private Slot lockUnusedLapisSlot(Slot original) {
        return new LockedIngredientSlot(original.container, original.slot, original.x, original.y);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    private void showFreeLapis(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        ItemStack lapis = new ItemStack(Items.LAPIS_LAZULI, FREE_LAPIS_COUNT);
        lapis.set(DataComponents.MAX_STACK_SIZE, FREE_LAPIS_COUNT);
        this.legacy_mechanics$menu().getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, FREE_LAPIS_COUNT));
    }

    @ModifyExpressionValue(
        method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;getItem(I)Lnet/minecraft/world/item/ItemStack;", ordinal = 1)
    )
    private ItemStack supplyUnusedLapis(ItemStack stack) {
        return new ItemStack(Items.LAPIS_LAZULI, MAX_ENCHANTMENT_POWER);
    }

    @Inject(method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z", at = @At("RETURN"))
    private void restockFreeLapis(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        this.legacy_mechanics$menu().getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, FREE_LAPIS_COUNT));
        this.legacy_mechanics$hideEnchantmentClue();
    }

    @Inject(method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z", at = @At("HEAD"))
    private void restoreEnchantmentClueForClick(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        this.legacy_mechanics$restoreEnchantmentClue();
    }

    @Inject(method = "removed(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"))
    private void discardFreeLapis(Player player, CallbackInfo ci) {
        this.legacy_mechanics$menu().getSlot(1).set(ItemStack.EMPTY);
    }

    @Inject(method = "slotsChanged(Lnet/minecraft/world/Container;)V", at = @At("RETURN"))
    private void hideEnchantmentClue(Container inventory, CallbackInfo ci) {
        this.legacy_mechanics$hiddenEnchantClue = this.enchantClue.clone();
        this.legacy_mechanics$hiddenLevelClue = this.levelClue.clone();
        this.legacy_mechanics$hideEnchantmentClue();
    }

    @Unique
    private void legacy_mechanics$restoreEnchantmentClue() {
        if (this.legacy_mechanics$hiddenEnchantClue.length == this.enchantClue.length
            && this.legacy_mechanics$hiddenLevelClue.length == this.levelClue.length) {
            System.arraycopy(this.legacy_mechanics$hiddenEnchantClue, 0, this.enchantClue, 0, this.enchantClue.length);
            System.arraycopy(this.legacy_mechanics$hiddenLevelClue, 0, this.levelClue, 0, this.levelClue.length);
        }
    }

    @Unique
    private void legacy_mechanics$hideEnchantmentClue() {
        for (int slot = 0; slot < this.enchantClue.length; slot++) {
            this.enchantClue[slot] = -1;
            this.levelClue[slot] = -1;
        }
        this.legacy_mechanics$menu().broadcastChanges();
    }

    @Unique
    private AbstractContainerMenu legacy_mechanics$menu() {
        return (AbstractContainerMenu) (Object) this;
    }
}
