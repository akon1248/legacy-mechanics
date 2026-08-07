package com.akon.legacymechanics.mixin.melee;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Restores 1.8-style melee: every swing lands fully charged, so damage is never
 * scaled down, crits are gated only on the jump check, and sweep attacks never fire.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    /**
     * High enough that the recharge delay (20 / attackSpeed ticks) stays well under
     * one tick even after a weapon's negative attack-speed modifier is applied, so
     * the client never draws a cooldown bar. Within ATTACK_SPEED's 0..1024 range.
     */
    @Unique
    private static final double UNCAPPED_ATTACK_SPEED = 100.0;

    /**
     * Overwrites the vanilla {@code .add(Attributes.ATTACK_SPEED)} entry -- the
     * builder is backed by {@code ImmutableMap.Builder.buildKeepingLast()}, so the
     * later value wins rather than throwing on the duplicate key.
     */
    @ModifyReturnValue(method = "createAttributes", at = @At("RETURN"))
    private static AttributeSupplier.Builder removeAttackSpeedDelay(AttributeSupplier.Builder builder) {
        return builder.add(Attributes.ATTACK_SPEED, UNCAPPED_ATTACK_SPEED);
    }

    /**
     * The single lever for the whole cooldown system: baseDamageScaleFactor, the
     * fully-charged gate in attack(), doSweepAttack's multiplier, stabAttack and the
     * Bukkit cooldown API all read through this.
     */
    @ModifyReturnValue(method = "getAttackStrengthScale(F)F", at = @At("RETURN"))
    private float alwaysFullyCharged(float scale) {
        return 1.0F;
    }

    @ModifyReturnValue(method = "cannotAttackWithItem(Lnet/minecraft/world/item/ItemStack;I)Z", at = @At("RETURN"))
    private boolean neverBlockAttack(boolean cannotAttack) {
        return false;
    }

    /**
     * Since 1.9 vanilla rejects a falling hit while sprinting. In 1.8 that sprint
     * state did not prevent a critical hit; the remaining falling/environment checks
     * in {@code canCriticalAttack} still apply.
     */
    @ModifyExpressionValue(
        method = "canCriticalAttack(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z")
    )
    private boolean allowSprintingCriticals(boolean sprinting) {
        return false;
    }

    /**
     * With the charge gate always open, sweeps would otherwise fire on every grounded
     * sword swing.
     */
    @ModifyReturnValue(method = "isSweepAttack(ZZZ)Z", at = @At("RETURN"))
    private boolean disableSweep(boolean isSweep) {
        return false;
    }
}
