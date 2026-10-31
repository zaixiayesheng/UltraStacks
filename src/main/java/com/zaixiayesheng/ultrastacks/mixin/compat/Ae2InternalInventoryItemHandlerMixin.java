package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 对外暴露的物品处理器视图 {@code InternalInventoryItemHandler}，单格上限同样由自己的 {@code getSlotLimit} 决定。
     *
 * <p>The item handler view AE2 exposes to other mods, {@code InternalInventoryItemHandler}, also decides
 * its per-slot limit through its own {@code getSlotLimit}.
 */
@Mixin(targets = "appeng.api.inventories.InternalInventoryItemHandler", remap = false)
public abstract class Ae2InternalInventoryItemHandlerMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.ae2SlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
