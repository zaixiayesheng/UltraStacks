package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙「合成升级」的合成格用自己的 {@code StatefulComponentItemHandler}，存储槽那一层的补丁到不了这里。
 *
 * <p>只把 {@code getSlotLimit} 返回的 64 与 99 抬到 {@link UltraStacksConfig#raiseSophisticatedLimit(int)} 的结果，其它值保持。
 *
 * <p>The crafting grid of the Sophisticated crafting upgrade uses its own
 * {@code StatefulComponentItemHandler}, which the storage-side patch cannot reach.
 *
 * <p>Only 64 and 99 returned by {@code getSlotLimit} are raised to the result of
 * {@link UltraStacksConfig#raiseSophisticatedLimit(int)}; every other value is kept.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.StatefulComponentItemHandler", remap = false)
public abstract class SophisticatedStatefulComponentItemHandlerMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }
}
