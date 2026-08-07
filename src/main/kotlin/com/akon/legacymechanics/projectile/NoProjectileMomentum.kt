package com.akon.legacymechanics.projectile

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.plugin.Plugin

/**
 * Disables the shooter's own velocity being added to a fired/thrown projectile's initial
 * velocity (`Projectile.shootFromRotation` adds `shooter.getKnownMovement()` on top of the throw
 * unless this is set) -- Paper's `misc.disable-relative-projectile-velocity` world setting, set
 * directly on each level's `WorldConfiguration` rather than shipped as YAML, since this project
 * ships behaviour through code. A moving, falling, or elytra-flying shooter's aim is then
 * independent of their own momentum.
 *
 * Set for every already-loaded world at [register], and for any world loaded afterward via
 * [WorldLoadEvent] -- the default worlds are already up by plugin `onEnable`, but this covers
 * one created later regardless.
 */
object NoProjectileMomentum : Listener {

    fun register(plugin: Plugin) {
        Bukkit.getWorlds().forEach(::apply)
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) = apply(event.world)

    private fun apply(world: World) {
        (world as CraftWorld).handle.paperConfig().misc.disableRelativeProjectileVelocity = true
    }
}
