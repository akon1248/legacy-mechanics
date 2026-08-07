package com.akon.legacymechanics.mixin.defaultcomponents;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets a vanilla item's default components be changed for the whole server -- arrows that stack
 * to 64, a sword with different attack modifiers, food with no eat cooldown -- so the change is
 * what the server itself computes, not a value patched onto individual stacks.
 *
 * <p>This is an accessor rather than an injector because the field, not the getter, is the real
 * seam. {@code Item.components()} is only one of the readers: {@code getDefaultMaxStackSize} and
 * {@code getName} read {@code this.components} directly (Item.java:155, 329), so a
 * {@code @ModifyReturnValue} on the getter would leave those two answering vanilla while
 * everything else answered the override. Replacing the field once makes every reader agree, and
 * costs nothing per call.
 *
 * <p>Writing it is safe despite {@code final}: the field is assigned once in the constructor and
 * never again, so nothing upstream caches a decision made from the old value. What does capture
 * it is {@link net.minecraft.core.component.PatchedDataComponentMap}, which holds the prototype
 * <i>by reference</i> from {@code ItemStack}'s constructor (ItemStack.java:286) -- which is why
 * {@link com.akon.legacymechanics.item.DefaultComponentOverrides} must run before any stack of the
 * item exists, and says so at more length.
 */
@Mixin(Item.class)
public interface ItemAccessor {

    @Mutable
    @Accessor("components")
    void setDefaultComponents(DataComponentMap components);
}
