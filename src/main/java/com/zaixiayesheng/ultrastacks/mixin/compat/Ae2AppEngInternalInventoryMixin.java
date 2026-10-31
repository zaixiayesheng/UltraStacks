package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 应用能源自己的库存实现。
     *
 * <p>AE2's own inventory backs the crafting grid of a terminal and the configuration slots of an
 * interface, and reports 64 as the limit of each slot, so it becomes the configured stack size. The
 * class is outside the mappings, which is why it is named through {@code targets} with
 * {@code remap = false}.
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
