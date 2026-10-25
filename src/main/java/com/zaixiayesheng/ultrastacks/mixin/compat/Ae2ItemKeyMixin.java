package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 应用能源的物品键报的最大堆叠数。
 *
 * <p>物品键对每个物品报的还是原版的 64，从终端取出整摞时会按这个值拆分，这里换成配置的堆叠上限。
 * 只改这一个查询，键本身与存储里已经记下的数量都不动。
 *
 * <p>The maximum stack size an AE2 item key reports. A key still reports the vanilla 64 per item, which is
 * what splits a full stack coming out of a terminal, so the query returns the configured stack size
 * instead. Only that query changes; the key and the amounts already stored are untouched.
 */
@Mixin(targets = "appeng.api.stacks.AEItemKey", remap = false)
public abstract class Ae2ItemKeyMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.ae2SlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
