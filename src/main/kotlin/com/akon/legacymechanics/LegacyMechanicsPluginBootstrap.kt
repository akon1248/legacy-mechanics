package com.akon.legacymechanics

import com.akon.fuel.loader.api.paper.DummyPluginBootstrap
import com.akon.legacymechanics.enchantment.DisabledEnchantmentTags
import io.papermc.paper.plugin.bootstrap.BootstrapContext

class LegacyMechanicsPluginBootstrap : DummyPluginBootstrap("LegacyMechanics", LegacyMechanicsEntrypoint) {
    override fun bootstrap(context: BootstrapContext) {
        DisabledEnchantmentTags.register(context)
    }
}
