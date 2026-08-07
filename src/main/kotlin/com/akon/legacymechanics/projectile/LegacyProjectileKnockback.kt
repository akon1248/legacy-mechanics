package com.akon.legacymechanics.projectile

import org.bukkit.Bukkit
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Egg
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.Entity
import org.bukkit.entity.FishHook
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Snowball
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.plugin.Plugin

/**
 * Adds knockback to three projectiles that deal none of it in vanilla, at any version: a fishing
 * hook pushes the entity it lands on immediately (not on reel-in), and snowballs/eggs -- which
 * always deal 0 damage -- push on hit too. None of this is a vanilla mechanic being restored; it's
 * the "rod PvP" style common on modified 1.8-era servers, built fresh since vanilla never had it.
 * Arrows are untouched -- they already carry their own (engine-default) knockback.
 */
object LegacyProjectileKnockback : Listener {

    fun register(plugin: Plugin) {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val target = event.hitEntity as? LivingEntity ?: return
        if (event.entity !is FishHook) return
        val builder = DamageSource.builder(DamageType.THROWN).withDirectEntity(event.entity)
        (event.entity.shooter as? LivingEntity)?.let { builder.withCausingEntity(it) }
        target.damage(Float.MIN_VALUE.toDouble(), builder.build())
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val damageSource = event.damageSource
        if (damageSource.damageType != DamageType.THROWN) return
        val source = damageSource.directEntity
        if (source is ThrowableProjectile && source !is ThrownPotion)
            event.damage = event.damage.coerceAtLeast(Float.MIN_VALUE.toDouble())
    }
}
