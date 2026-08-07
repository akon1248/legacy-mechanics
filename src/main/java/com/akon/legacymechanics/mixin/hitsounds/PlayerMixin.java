package com.akon.legacymechanics.mixin.hitsounds;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Silences only player melee attack sounds, retaining all other player and world sound effects. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @WrapOperation(
        method = "attack(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V")
    )
    private void silenceAttackAndKnockbackSounds(Player player, SoundEvent sound, Operation<Void> original) {
    }

    @WrapOperation(
        method = "attackVisualEffects(Lnet/minecraft/world/entity/Entity;ZZZZF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V")
    )
    private void silenceNormalAndCriticalHitSounds(Player player, SoundEvent sound, Operation<Void> original) {
    }
}
