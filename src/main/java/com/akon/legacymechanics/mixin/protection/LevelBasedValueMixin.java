package com.akon.legacymechanics.mixin.protection;

import com.akon.legacymechanics.enchantment.FloorLevelBasedValue;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Registers {@code annihilation:floor}, so the protection-family enchantment jsons can express
 * the exact 1.8.8 EPF formula for every level instead of pre-baking four levels into a lookup
 * table -- see {@link FloorLevelBasedValue}.
 */
@Mixin(LevelBasedValue.class)
public interface LevelBasedValueMixin {

    @Inject(
        method = "bootstrap(Lnet/minecraft/core/Registry;)Lcom/mojang/serialization/MapCodec;",
        at = @At("RETURN")
    )
    private static void registerFloorCombinator(
        Registry<MapCodec<? extends LevelBasedValue>> registry,
        CallbackInfoReturnable<MapCodec<? extends LevelBasedValue>> cir
    ) {
        Registry.register(registry, "annihilation:floor", FloorLevelBasedValue.CODEC);
    }
}
