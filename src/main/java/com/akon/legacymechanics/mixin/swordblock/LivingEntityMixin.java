package com.akon.legacymechanics.mixin.swordblock;

import com.akon.legacymechanics.item.SwordBlocking;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A sword-block is a damage reduction, not a shield: it must not trip the game's "this hit was
 * blocked" signalling that a real shield relies on. That signalling all derives from one dry-run
 * probe in {@code hurtServer} -- {@code float f1 = this.applyItemBlocking(level, damageSource,
 * amount, true);} -- which becomes {@code boolean flag = f1 > 0.0F} and from there drives:
 *
 * <ul>
 *   <li>the block sound -- {@code flag} picks {@code BlocksAttacks.onBlocked} (shield-clang) over
 *       {@code level.broadcastDamageEvent} (normal hurt sound)</li>
 *   <li>{@code hurtServer}'s return value ({@code !flag}) -- read by {@code Player.attack} to
 *       decide whether to apply the attacker's bonus knockback, and by arrows' {@code
 *       onHitEntity} to decide whether to deflect off the target instead of embedding normally</li>
 * </ul>
 *
 * Zeroing this one probe result for a sword-block makes {@code flag} false, so both follow
 * automatically -- without touching the real damage number, which CraftBukkit's {@code
 * handleEntityDamage} computes separately and independently of this local. This intentionally
 * targets only this one call site rather than {@code hurtServer}'s return value directly: that
 * method has several unrelated early {@code return false} guards (invulnerability, already-dead,
 * fire resistance, the i-frame/last-hurt check) that all run before this probe, so a return-value
 * mixin would misfire on those too. This call happens strictly after all of them.
 *
 * One vanilla behaviour rides on that same {@code flag} but should keep the *old* (shield-like)
 * outcome rather than follow it: {@code indicateDamage} -- the client's hurt-direction screen
 * flash ({@code ClientboundHurtAnimationPacket}) -- is only sent when {@code !flag}, so once
 * {@code flag} is forced false for swords it would start firing on every sword-block, which is a
 * behaviour change nobody asked for. That call is wrapped separately, re-reading {@code
 * getUseItem()} off the receiver rather than capturing {@code hurtServer}'s {@code useItem} local
 * via {@code @Local}: that local's slot does not survive in the compiled LVT down to the
 * {@code indicateDamage} call site (verified by running -- {@code @Local} there throws
 * {@code ArrayIndexOutOfBoundsException} in Mixin's local discriminator at class-transform time,
 * which fails the *entire* mixin class, silently taking the other handler down with it).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    public abstract ItemStack getUseItem();

    @ModifyExpressionValue(
        method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;applyItemBlocking(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;FZ)F"
        )
    )
    private float dontSignalBlockedForSword(float blockedAmount) {
        if (blockedAmount <= 0.0F) return blockedAmount;
        return SwordBlocking.isSword(this.getUseItem()) ? 0.0F : blockedAmount;
    }
}
