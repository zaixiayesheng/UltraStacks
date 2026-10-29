package com.zaixiayesheng.ultrastacks.mixin.compat;

import appeng.api.stacks.AEKey;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ME 接口、样板供应器和其它应用能源机器共用的通用库存。
 *
 * <p>它对物品键报的每槽容量是 64，接口一次能搬多少就看这个值，所以这里改成配置上限的十六分之一
 * （与自动的药水上限同一个比例）。
 *
 * <p>The generic inventory shared by ME interfaces, pattern providers and other AE2 machines. It reports
 * 64 as the per-slot capacity for item keys, which is what limits interface throughput, so the capacity
 * becomes a sixteenth of the configured stack size, the same ratio used for the automatic potion limit.
 */
@Mixin(targets = "appeng.helpers.externalstorage.GenericStackInv", remap = false)
public abstract class Ae2GenericStackInvMixin {

    @Inject(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseMaxAmount(AEKey key, CallbackInfoReturnable<Long> cir) {
        long original = cir.getReturnValueJ();
        long raised = UltraStacksConfig.ae2TransferAmount(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
