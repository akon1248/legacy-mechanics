package com.akon.legacymechanics.mixin.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes Strength and Weakness scale total melee damage as they did in 1.8. */
@Mixin(MobEffect.AttributeTemplate.class)
public abstract class MobEffectAttributeTemplateMixin {

    @Shadow
    public abstract Identifier id();

    @Inject(method = "create(I)Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;", at = @At("HEAD"), cancellable = true)
    private void restoreLegacyDamageMultipliers(int level, CallbackInfoReturnable<AttributeModifier> cir) {
        Identifier id = this.id();
        if (id.equals(Identifier.withDefaultNamespace("effect.strength"))) {
            cir.setReturnValue(new AttributeModifier(id, 1.3D * (level + 1), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (id.equals(Identifier.withDefaultNamespace("effect.weakness"))) {
            cir.setReturnValue(new AttributeModifier(id, -0.5D * (level + 1), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}