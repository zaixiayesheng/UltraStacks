package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.items.ComponentItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge 组件物品槽（{@code ComponentItemHandler}）的槽位上限。
     *
 * <p>Slot limit for NeoForge's item-component backed handler {@code ComponentItemHandler}. Its
 * default implementation returns {@code Item.ABSOLUTE_MAX_STACK_SIZE}, which is 99, so an item
 * that stacks higher is still capped at 99 when inserted. The hook substitutes
 * {@code max(99, configured limit)} and never lowers the value, so a larger stack already in the
 * slot stays valid.
 */
@Mixin(ComponentItemHandler.class)
public abstract class ComponentItemHandlerMixin {
    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int ultrastacks$raiseModdedLimit(int original) {
        return UltraStacksConfig.raiseModdedSlotLimit(original);
    }
}
