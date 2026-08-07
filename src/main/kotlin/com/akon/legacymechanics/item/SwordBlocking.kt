package com.akon.legacymechanics.item

import net.minecraft.core.component.DataComponents
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.BlocksAttacks
import java.util.Optional

/**
 * Restores 1.8-style sword blocking: holding right-click with any sword raises it and halves
 * incoming damage, with no windup and no durability cost.
 *
 * Nothing here is a mixin. Since 1.21.5 blocking is entirely component-driven -- the base [Item]
 * class checks `BLOCKS_ATTACKS` in all three places that matter, and swords are plain `Item`s
 * built by `Properties.sword(...)` with no subclass of their own:
 *
 * - `Item.use` (`Item.java:192`) calls `startUsingItem` for anything carrying the component
 * - `Item.getUseAnimation` (`Item.java:295`) returns `BLOCK`
 * - `Item.getUseDuration` (`Item.java:307`) returns 72000 ticks, i.e. hold-to-block
 *
 * So giving swords the component is the whole feature. Registered as a changed *default* rather
 * than stamped onto individual stacks so that every sword in the game has it, including ones
 * already in chests and ones other code creates without knowing about this.
 */
object SwordBlocking {

    /**
     * Tuned against 1.8 sword blocking rather than against the modern shield, which is the point
     * of the feature. Differences from `Items.SHIELD`, each deliberate:
     *
     * - **No windup.** The shield's 0.25s `blockDelaySeconds` is what makes modern blocking feel
     *   committal; 1.8 blocking was instant, and the whole reason to want it back.
     * - **50% reduction, not 100%** (`factor` 0.5 against the shield's 1.0). A sword that negated
     *   damage outright would be strictly better than a shield while also being a weapon.
     * - **No durability cost** (`base` and `factor` both 0, so `apply` always returns 0 and
     *   `hurtBlockingItem` never calls `hurtAndBreak`). 1.8 blocking was free; charging durability
     *   would also quietly nerf the sword's real job.
     *
     * Unlike the shield, the block arc is a full circle: 1.8 swords blocked attacks from every
     * direction. `BYPASSES_SHIELD` remains honoured, so `/kill` and comparable damage still win.
     */
    private val SWORD_BLOCK = BlocksAttacks(
        0.0f,
        0.0f,
        listOf(BlocksAttacks.DamageReduction(360.0f, Optional.empty(), 0.0f, 0.5f)),
        BlocksAttacks.ItemDamageFunction(0.0f, 0.0f, 0.0f),
        Optional.of(DamageTypeTags.BYPASSES_SHIELD),
        Optional.empty(),
        Optional.empty(),
    )

    /**
     * Listed explicitly rather than read from `ItemTags.SWORDS`, which is not an option here:
     * tag *contents* come from datapacks loaded at world load, long after the PostBootstrap
     * window this runs in. At registration time the tag exists but is empty.
     */
    private val SWORDS = listOf(
        Items.WOODEN_SWORD,
        Items.STONE_SWORD,
        Items.COPPER_SWORD,
        Items.IRON_SWORD,
        Items.GOLDEN_SWORD,
        Items.DIAMOND_SWORD,
        Items.NETHERITE_SWORD,
    )

    fun register() {
        SWORDS.forEach { DefaultComponentOverrides.override(it, DataComponents.BLOCKS_ATTACKS, SWORD_BLOCK) }
    }

    /** Used by [com.akon.legacymechanics.mixin.swordblock.LivingEntityMixin] to tell a sword-block apart from a shield-block. */
    @JvmStatic
    fun isSword(stack: ItemStack): Boolean = SWORDS.contains(stack.item)
}
