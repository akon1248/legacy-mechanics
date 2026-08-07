package com.akon.legacymechanics.item

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Items

/** Restores the 1.8 direct-impact splash-potion duration of 75% of the base effect. */
object ShorterSplashPotionDurations {
    fun register() {
        // ThrownSplashPotion multiplies the effect duration by this component and by splash
        // proximity. A 0.75 default therefore restores the direct-hit duration while retaining
        // vanilla falloff for targets away from the impact.
        DefaultComponentOverrides.override(Items.SPLASH_POTION, DataComponents.POTION_DURATION_SCALE, 0.75F)
    }
}
