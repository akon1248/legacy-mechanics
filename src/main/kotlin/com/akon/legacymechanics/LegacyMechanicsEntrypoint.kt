package com.akon.legacymechanics

import org.bukkit.plugin.Plugin
import com.akon.legacymechanics.item.DefaultComponentOverrides
import com.akon.legacymechanics.item.LegacyPotionColors
import com.akon.legacymechanics.item.PotionGlint
import com.akon.legacymechanics.item.LegacyPotionTooltips
import com.akon.legacymechanics.item.SharpnessTooltip
import com.akon.legacymechanics.player.PersistentSprint
import com.akon.legacymechanics.projectile.LegacyProjectileKnockback
import com.akon.legacymechanics.projectile.NoProjectileMomentum
import com.akon.fuel.loader.api.Entrypoint
import com.akon.fuel.loader.api.FuelMod
import com.akon.fuel.loader.api.events.FuelEvents
import com.akon.fuel.loader.api.events.PostBootstrapEvent
import com.akon.fuel.loader.api.paper.PluginLifecycleDelegate

@FuelMod
object LegacyMechanicsEntrypoint : PluginLifecycleDelegate {

    @Entrypoint
    fun onBootstrap() {
        LegacyPotionTooltips.register()
        LegacyPotionColors.register()
        PotionGlint.register()
        SharpnessTooltip.register()
        FuelEvents.BUS.addListener<PostBootstrapEvent> {
            DefaultComponentOverrides.registerDefaults()
        }
    }

    override fun onEnable(plugin: Plugin) {
        NoProjectileMomentum.register(plugin)
        LegacyProjectileKnockback.register(plugin)
        PersistentSprint.register(plugin)
    }
}
