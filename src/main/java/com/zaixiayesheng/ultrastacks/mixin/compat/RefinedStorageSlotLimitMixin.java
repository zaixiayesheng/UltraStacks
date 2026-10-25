package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精致存储的物品处理器。
 *
 * <p>终端格子和网络库存都包在它里面，它报的上限跟着底层是 64 或 99。合成格本身是原版 {@code Slot}，
 * 已由通用 mixin 处理，这里补的是处理器这一层。
 *
 * <p>The item handler Refined Storage wraps grid slots and network storage in. It reports 64 or 99,
 * following whatever the underlying inventory says. The crafting grid itself is a vanilla {@code Slot}
 * and is already covered by the generic mixin, so what is patched here is the handler layer.
 */
@Mixin(targets = "com.refinedmods.refinedstorage.inventory.item.ProxyItemHandler", remap = false)
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
