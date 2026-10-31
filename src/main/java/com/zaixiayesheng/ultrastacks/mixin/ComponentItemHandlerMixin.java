package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.items.ComponentItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 把物品组件式物品槽的每格上限抬到配置的堆叠上限。
     *
 * <p>Raises the per-slot limit of component-backed item slots to the configured stack size.
 */
@Mixin(ComponentItemHandler.class)
public abstract class ComponentItemHandlerMixin {
    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int ultrastacks$raiseModdedLimit(int original) {
        return UltraStacksConfig.raiseModdedSlotLimit(original);
    }
}
