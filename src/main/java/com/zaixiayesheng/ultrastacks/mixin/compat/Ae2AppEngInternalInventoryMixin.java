package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 自己的库存实现 {@code AppEngInternalInventory}，单格上限由 {@code getSlotLimit} 报告，数值来自它内部的一张 int 数组。
     *
 * <p>AE2's own inventory implementation {@code AppEngInternalInventory} reports a per-slot limit from
 * {@code getSlotLimit}, backed by an int array it keeps internally.
 */
@Mixin(targets = "appeng.util.inv.AppEngInternalInventory", remap = false)
public abstract class Ae2AppEngInternalInventoryMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.ae2SlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
