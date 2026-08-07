package com.akon.legacymechanics.mixin.armor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the ARMOR attribute use the 1.8 fixed 4%-per-point reduction for every entity.
 *
 * This is intentionally material-agnostic: armour source and toughness no longer influence the
 * formula; only the resolved ARMOR attribute value does. It still runs the legacy ratio through
 * {@link EnchantmentHelper#modifyArmorEffectiveness} -- vanilla's own weapon-side hook for
 * enchantments like Breach -- because that call, not the toughness formula, is what those
 * enchantments are built on; skipping it would silently disable them rather than restore anything.
 */
@Mixin(CombatRules.class)
public abstract class CombatRulesMixin {
    @Inject(
        method = "getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void useLegacyArmorAttributeFormula(
        LivingEntity entity,
        float damage,
        DamageSource source,
        float armor,
        float toughness,
        CallbackInfoReturnable<Float> cir
    ) {
        float legacyRatio = Math.min(20.0F, armor) / 25.0F;
        ItemStack weapon = source.getWeaponItem();
        float effectiveness;
        if (weapon != null && entity.level() instanceof ServerLevel serverLevel) {
            effectiveness = Mth.clamp(
                EnchantmentHelper.modifyArmorEffectiveness(serverLevel, weapon, entity, source, legacyRatio),
                0.0F,
                1.0F
            );
        } else {
            effectiveness = legacyRatio;
        }
        cir.setReturnValue(damage * (1.0F - effectiveness));
    }
}
