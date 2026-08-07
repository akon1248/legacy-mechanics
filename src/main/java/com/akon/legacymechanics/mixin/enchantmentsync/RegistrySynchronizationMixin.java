package com.akon.legacymechanics.mixin.enchantmentsync;

import com.akon.legacymechanics.network.OutgoingEnchantmentEffects;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Keeps a client with no `annihilation:...` enchantment-effect types registered from crashing on
 * login: strips any effect built from one of them out of the registry-sync payload instead of
 * sending it and letting that client's codec fail to decode it.
 */
@Mixin(RegistrySynchronization.class)
public abstract class RegistrySynchronizationMixin {

    /**
     * {@code packRegistries} is the public entry point that fans out into the private,
     * lambda-heavy {@code packRegistry} per synced registry -- wrapping the sender here reaches
     * every one of those calls without targeting a compiler-named lambda method inside it.
     */
    @ModifyVariable(
            method = "packRegistries(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/core/RegistryAccess;Ljava/util/Set;Ljava/util/function/BiConsumer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            name = "packetSender")
    private static BiConsumer<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> sanitizeBeforeSend(
        BiConsumer<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> packetSender
    ) {
        return (key, entries) -> packetSender.accept(key, OutgoingEnchantmentEffects.sanitize(key, entries));
    }
}
