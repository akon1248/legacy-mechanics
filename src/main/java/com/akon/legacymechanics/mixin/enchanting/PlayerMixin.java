package com.akon.legacymechanics.mixin.enchanting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Makes a table enchantment consume its displayed power, as it did before 1.8. */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Shadow
    public AbstractContainerMenu containerMenu;

    @ModifyVariable(
            method = "onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            name = "levelCost")
    private int spendDisplayedEnchantmentPower(int modernTier) {
        if (this.containerMenu instanceof EnchantmentMenu menu) {
            return menu.costs[modernTier - 1];
        }
        return modernTier;
    }
}
