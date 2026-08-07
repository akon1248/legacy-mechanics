package com.akon.legacymechanics.mixin.damageparticles;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Landing a hit no longer puffs out the gray damage-indicator particles; hit
 * feedback is left to the sound and the hurt flash alone.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    /** What {@code sendParticles} returns when it reaches nobody: zero players notified. */
    @Unique
    private static final int NO_PARTICLES_SENT = 0;

    /**
     * The particles are the only effect being dropped -- the surrounding method also
     * awards the DAMAGE_DEALT statistic, so the call is skipped rather than the whole
     * method cancelled.
     */
    @WrapOperation(
        method = "damageStatsAndHearts(Lnet/minecraft/world/entity/Entity;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"
        )
    )
    private int noDamageIndicatorParticles(
        ServerLevel level,
        ParticleOptions options,
        double x,
        double y,
        double z,
        int count,
        double xDist,
        double yDist,
        double zDist,
        double speed,
        Operation<Integer> original
    ) {
        return NO_PARTICLES_SENT;
    }
}
