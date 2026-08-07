package com.akon.legacymechanics.mixin.freeblazepowder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes brewing stands run on the free blaze powder shown in their locked fuel slot. */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {

    @Unique
    private static final int FREE_FUEL_USES = 20;

    @Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;)V", at = @At("HEAD"))
    private static void replenishFreeFuel(Level level, BlockPos pos, BlockState state,
                                          BrewingStandBlockEntity brewingStand, CallbackInfo ci) {
        brewingStand.fuel = FREE_FUEL_USES;
    }
}
