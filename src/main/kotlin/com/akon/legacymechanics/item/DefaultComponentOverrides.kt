package com.akon.legacymechanics.item

import com.akon.legacymechanics.mixin.defaultcomponents.ItemAccessor
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory

/**
 * Changed default components for vanilla items -- a bigger stack limit on arrows, a different
 * tool rule, no food cooldown -- expressed once and made to hold on both sides of the wire.
 *
 * [override] does both halves. The server-side half replaces `Item.components` outright, so
 * everything that asks the item what its defaults are gets the new answer. The client-side half
 * is [forceOntoWire], called from the outgoing-item pipeline.
 *
 * ## Why the wire needs its own step at all
 *
 * An ItemStack goes over the network as a *patch* relative to the item's default components,
 * not as a resolved map. A vanilla client resolves that patch against its own vanilla defaults,
 * so a default we changed server-side is by construction invisible: it is exactly the part that
 * never gets transmitted. Making the client agree means writing the changed value into the
 * outgoing patch explicitly, which is what [forceOntoWire] does.
 *
 * ## When [override] may be called
 *
 * **Only from [registerDefaults], which runs on `PostBootstrapEvent`.** `ItemStack` does not
 * consult `Item.components()` per access; it captures the map *by reference* into its
 * `PatchedDataComponentMap` at construction (`ItemStack.java:286`). A stack built before the
 * swap therefore keeps resolving against the old defaults for its whole life, while
 * [forceOntoWire] -- which reads this registry, not the stack's prototype -- would still force
 * the new value onto the wire for it. That combination desyncs client from server on exactly
 * the stacks that predate the call, which is the one failure this whole design exists to avoid.
 *
 * `PostBootstrapEvent` is the one window with no such stacks to get wrong: fuel-loader posts it
 * from `Main.main` immediately after `Bootstrap.validate()`, so the item registry is fully
 * populated and the `MinecraftServer` does not yet exist, let alone a loaded world.
 */
object DefaultComponentOverrides {

    private val LOGGER = LoggerFactory.getLogger(DefaultComponentOverrides::class.java)

    @Volatile
    private var overrides: Reference2ObjectMap<Item, DataComponentMap> = Reference2ObjectMaps.emptyMap()

    /**
     * Each item's components as vanilla built them, captured the first time it is overridden.
     * Merges are always computed from this rather than from the item's current map, so
     * overriding the same item twice does not compound onto an already-merged result -- and so
     * a later override of one component cannot resurrect an earlier one that was replaced.
     */
    private val vanillaDefaults = Reference2ObjectOpenHashMap<Item, DataComponentMap>()

    /** True only for the duration of [registerDefaults]; see the class doc for why it matters. */
    private var registering = false

    /**
     * The one place overridden defaults are declared. Called on `PostBootstrapEvent` from
     * [com.akon.legacymechanics.LegacyMechanicsEntrypoint] -- see the class doc for why that is the
     * only safe window, and why this is a fixed list rather than something other code registers into.
     */
    fun registerDefaults() {
        registering = true
        try {
            SwordBlocking.register()
            GoldenAppleEffects.register()
            LegacyItemAttributes.register()
            ShorterSplashPotionDurations.register()
            NoItemCooldowns.register()
        } finally {
            registering = false
        }
        // Logged even at zero: it is the only boot-time evidence that PostBootstrap fired and
        // the @Entrypoint subscription is still wired up. A silent boot and a broken hook look
        // identical from the log otherwise.
        LOGGER.info("Applied default component overrides for {} item(s)", overrides.size)
    }

    /**
     * Registers [value] as [item]'s new default for [type] and applies it to the item
     * immediately. See the class doc for the (load-bearing) constraint on *when* this may run.
     *
     * Main thread only. Reads happen on netty threads and are unsynchronized, kept safe by
     * publishing a whole new map through the volatile rather than mutating the live one.
     *
     * No validation is performed. Writing the field bypasses
     * `Item.Properties.buildAndValidateComponents` and its durability-plus-stackable check
     * (`Item.java:640`); that check was briefly repeated here and was **deliberately removed** on
     * 2026-07-19. Vanilla only applies it when *constructing* an item, while the same end state
     * is reachable at any time through ordinary commands
     * (`/give ...[minecraft:max_stack_size=2]` on a damageable item), so it does not guard an
     * invariant vanilla actually maintains — it just made this path stricter than the game.
     * Do not reinstate it without revisiting that reasoning.
     */
    fun <T : Any> override(item: Item, type: DataComponentType<T>, value: T) {
        if (!registering) {
            // Warns rather than throws: a probe script deliberately calls this late to exercise
            // the mechanism, and that stays useful. Anything shipping belongs in
            // registerDefaults -- outside it the swap is unsound, not merely untidy.
            LOGGER.warn(
                "Default component override for {} registered outside PostBootstrap; stacks that " +
                    "already exist keep the old defaults and will desync from what clients are sent.",
                item,
            )
        }

        val updated = Reference2ObjectOpenHashMap(overrides)
        val existing = updated[item]
        val forItem = DataComponentMap.builder()
            .apply { if (existing != null) addAll(existing) }
            .set(type, value)
            .build()
        updated[item] = forItem

        val vanilla = vanillaDefaults.getOrPut(item) { item.components() }
        val merged = DataComponentMap.builder().addAll(vanilla).addAll(forItem).build()

        // Built flat rather than via DataComponentMap.composite: composite's keySet is a lazy
        // Sets.union view, and this map is read on every ItemStack construction. A flat build
        // is the same shape vanilla produced, so lookup cost is unchanged.
        (item as ItemAccessor).setDefaultComponents(merged)
        overrides = updated
    }

    /** The overridden defaults for [item], or null if it has none. */
    fun forItem(item: Item): DataComponentMap? = overrides[item]

    /**
     * Returns what should go on the wire in place of [stack], with every overridden default
     * written in explicitly so the receiving client resolves the same values the server does.
     *
     * A component the stack already carries in its own patch is left alone: that value is
     * transmitted anyway, and it is the more specific answer. That is also what makes this safe
     * to run after [com.akon.legacymechanics.network.OutgoingItemStackEvent] listeners -- anything
     * a listener set or removed is in the patch by then, so listeners win over defaults.
     *
     * Allocates nothing for the overwhelmingly common case of an item with no overrides, or one
     * whose overridden components the stack already sets.
     */
    @JvmStatic
    fun forceOntoWire(stack: ItemStack): ItemStack {
        val all = overrides
        if (all.isEmpty() || stack.isEmpty) return stack
        val forced = all[stack.item] ?: return stack

        val patch = stack.componentsPatch
        var builder: DataComponentPatch.Builder? = null
        for (component in forced) {
            if (patch.get(component.type()) != null) continue
            val target = builder ?: DataComponentPatch.builder().also { it.copy(patch); builder = it }
            target.setTyped(component)
        }
        val built = builder ?: return stack

        val copy = stack.copy()
        // restorePatch, not applyPatch/set: those compare against the prototype and drop any
        // entry equal to it, which -- once the server-side default is the overridden value --
        // is every entry we are trying to add. restorePatch writes the map verbatim.
        // Safe on a copy: it never touches the live stack, and nothing here is persisted.
        (copy.components as PatchedDataComponentMap).restorePatch(built.build())
        return copy
    }

    /** Bridges the star projection [forced]'s iterator yields into Builder's `set(TypedDataComponent<T>)`. */
    @Suppress("UNCHECKED_CAST")
    private fun DataComponentPatch.Builder.setTyped(component: TypedDataComponent<*>) {
        set(component as TypedDataComponent<Any>)
    }
}
