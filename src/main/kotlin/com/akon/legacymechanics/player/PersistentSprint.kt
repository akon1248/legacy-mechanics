package com.akon.legacymechanics.player

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.plugin.Plugin

/**
 * Keeps a player sprinting through a sprint or Knockback-enchanted melee hit -- Paper's
 * `misc.disable-sprint-interruption-on-attack` world setting, set directly on each level's
 * `WorldConfiguration` rather than shipped as YAML, since this project ships behaviour through
 * code. Without it, `Player.causeExtraKnockback` cancels sprinting on the attacker mid-swing.
 *
 * Set for every already-loaded world at [register], and for any world loaded afterward via
 * [WorldLoadEvent] -- the default worlds are already up by plugin `onEnable`, but this covers
 * one created later regardless.
 */
object PersistentSprint : Listener {

    fun register(plugin: Plugin) {
        Bukkit.getWorlds().forEach(::apply)
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) = apply(event.world)

    private fun apply(world: World) {
        (world as CraftWorld).handle.paperConfig().misc.disableSprintInterruptionOnAttack = true
    }
}
