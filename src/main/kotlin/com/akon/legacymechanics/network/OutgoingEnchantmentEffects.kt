package com.akon.legacymechanics.network

import net.minecraft.core.Registry
import net.minecraft.core.RegistrySynchronization
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import java.util.Optional

/**
 * Strips effect entries built from a non-vanilla `type` (a custom `LevelBasedValue` or
 * `EnchantmentValueEffect` registered only on this server) out of the `Enchantment` payload
 * before it reaches a client during registry sync. `Enchantment.DIRECT_CODEC` is the wire format
 * too (`RegistryDataLoader.SYNCHRONIZED_REGISTRIES`), so a client without our mixin cannot decode
 * a custom type key.
 *
 * Dropping rather than translating is safe here, not just convenient: every effect component
 * Mojang ships is a `List` (`EnchantmentEffectComponents`), so removing one entry -- down to
 * zero, if need be -- always leaves a structurally valid, vanilla-decodable `Enchantment`. And
 * every combat effect (`damage_protection` included) is server-authoritative: the client never
 * evaluates its own copy for anything, it only receives one because the sync wire format has no
 * per-target field filtering, only per-registry.
 */
object OutgoingEnchantmentEffects {

    private const val EFFECTS_KEY = "effects"
    private const val TYPE_KEY = "type"
    private const val VANILLA_NAMESPACE = "minecraft:"

    @JvmStatic
    fun sanitize(
        registryKey: ResourceKey<out Registry<*>>,
        entries: List<RegistrySynchronization.PackedRegistryEntry>
    ): List<RegistrySynchronization.PackedRegistryEntry> {
        if (registryKey != Registries.ENCHANTMENT) return entries

        var changed = false
        val sanitized = entries.map { entry ->
            val tag = entry.data().orElse(null) as? CompoundTag ?: return@map entry
            val sanitizedTag = sanitizeEnchantment(tag)
            if (sanitizedTag === tag) {
                entry
            } else {
                changed = true
                RegistrySynchronization.PackedRegistryEntry(entry.id(), Optional.of(sanitizedTag))
            }
        }
        return if (changed) sanitized else entries
    }

    private fun sanitizeEnchantment(enchantment: CompoundTag): CompoundTag {
        val effects = enchantment.get(EFFECTS_KEY) as? CompoundTag ?: return enchantment

        var changed = false
        val sanitizedEffects = CompoundTag()
        for (component in effects.keySet()) {
            when (val value = effects.get(component)) {
                null -> {}
                is ListTag -> {
                    val pruned = pruneForeignEntries(value)
                    if (pruned !== value) changed = true
                    sanitizedEffects.put(component, pruned)
                }
                else -> if (hasForeignType(value)) {
                    changed = true
                } else {
                    sanitizedEffects.put(component, value)
                }
            }
        }
        if (!changed) return enchantment

        val copy = enchantment.copy()
        copy.put(EFFECTS_KEY, sanitizedEffects)
        return copy
    }

    private fun pruneForeignEntries(list: ListTag): ListTag {
        var changed = false
        val kept = ListTag()
        for (element in list) {
            if (hasForeignType(element)) changed = true else kept.add(element)
        }
        return if (changed) kept else list
    }

    private fun hasForeignType(tag: Tag): Boolean = when (tag) {
        is CompoundTag -> {
            val type = tag.get(TYPE_KEY)
            (type is StringTag && !type.value().startsWith(VANILLA_NAMESPACE)) ||
                tag.keySet().any { key -> tag.get(key)?.let(::hasForeignType) == true }
        }
        is ListTag -> tag.any(::hasForeignType)
        else -> false
    }
}
