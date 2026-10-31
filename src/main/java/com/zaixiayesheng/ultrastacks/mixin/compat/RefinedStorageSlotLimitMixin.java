package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精致存储把终端格子与网络库存包成 {@code InsertExtractItemHandler}，单格上限由这一层自己报。
     *
 * <p>Refined Storage wraps its grid slots and network storage in {@code InsertExtractItemHandler}, and
 * this layer reports the per-slot limit on its own.
 */
@Mixin(targets = "com.refinedmods.refinedstorage.neoforge.support.inventory.InsertExtractItemHandler", remap = false)
public abstract class RefinedStorageSlotLimitMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.refinedStorageSlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
