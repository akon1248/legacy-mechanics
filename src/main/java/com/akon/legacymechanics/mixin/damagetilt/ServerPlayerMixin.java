package com.akon.legacymechanics.mixin.damagetilt;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Restores, on purpose, Minecraft's own decade-long "damage tilt" bug (MC-26678 / MC-202355): from
 * 1.3.1 (2012), when singleplayer and multiplayer were merged onto one client-server codebase,
 * until the fix in 23w03a/1.19.4, the hurt-camera screen tilt was supposed to lean toward the
 * attacker but never actually did. Verified against 1.8.8's own decompiled source (MCP918): the
 * server computed the real direction into a purely server-local field (`EntityLivingBase#attackEntityFrom`),
 * then told the client about the hit via nothing but a generic 1-byte "entity status 2" packet
 * (`Entity#setEntityState`) -- no direction payload existed. The client's handler for that status
 * byte (`EntityLivingBase#handleStatusUpdate`) unconditionally zeroed the field instead:
 * {@code this.attackedAtYaw = 0.0F;}. So the value actually in effect, every hit, for a decade,
 * was exactly {@code 0.0F} -- never the real computed direction.
 *
 * This reproduces that exact value for nostalgia, not the original defect's mechanism: rather than
 * reintroducing a real sync gap, it simply pins the value {@code ServerPlayer.indicateDamage}
 * sends the client to {@code 0.0F} regardless of the real attacker direction.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /** The value 1.8.8's client actually used for every hit -- see class doc. */
    @Unique
    private static final float NOSTALGIC_HURT_DIR = 0.0F;

    @Redirect(
        method = "indicateDamage(DD)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;hurtDir:F", opcode = Opcodes.PUTFIELD)
    )
    private void alwaysTiltTheSameWay(Player player, float computedHurtDir) {
        player.hurtDir = NOSTALGIC_HURT_DIR;
    }
}
