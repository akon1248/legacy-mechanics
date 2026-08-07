package com.akon.legacymechanics.mixin.food;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Restores the 1.8 four-exhaustion cost for the 18-food natural-regeneration branch. */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    /** The first isHurt call belongs only to the saturated fast-regeneration condition. */
    @ModifyExpressionValue(
        method = "tick(Lnet/minecraft/server/level/ServerPlayer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isHurt()Z", ordinal = 0)
    )
    private boolean disableSaturatedNaturalRegeneration(boolean isHurt) {
        return false;
    }

    /**
     * Ordinal one is the unsaturated branch in FoodData.tick(ServerPlayer): ordinal zero is the
     * saturated branch and deliberately keeps its modern minimum-saturation exhaustion.
     */
    @ModifyArg(
        method = "tick(Lnet/minecraft/server/level/ServerPlayer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(FLorg/bukkit/event/entity/EntityExhaustionEvent$ExhaustionReason;)V", ordinal = 1),
        index = 0
    )
    private float restoreUnsaturatedRegenExhaustion(float exhaustion) {
        return 4.0F;
    }
}
