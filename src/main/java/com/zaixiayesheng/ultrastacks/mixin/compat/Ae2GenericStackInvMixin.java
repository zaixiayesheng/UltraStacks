package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AE2 的通用库存 {@code GenericStackInv} 对每个槽位按 key 报一个容量，{@code getMaxAmount} 就是它。
 *
 * <p>原值为 64 或 99 时返回 {@code max(原值, 上限 / 16)}，其它值原样返回：机器写死的更大容量不能被压小。除以 16 让单次搬运量随配置上限放大，默认配置下是 4096。
 *
 * <p>AE2's generic inventory {@code GenericStackInv} reports a capacity per slot for a given key through
 * {@code getMaxAmount}.
 *
 * <p>A value of 64 or 99 becomes {@code max(value, limit / 16)} and anything else is passed through, so
 * a larger capacity hardcoded by a machine is not lowered. The division by 16 lets a single transfer
 * grow with the configured limit; with the default config it yields 4096.
 */
@Mixin(targets = "appeng.helpers.externalstorage.GenericStackInv", remap = false)
public abstract class Ae2GenericStackInvMixin {

    @ModifyReturnValue(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), require = 0)
    private long ultrastacks$raiseMaxAmount(long original) {
        return UltraStacksConfig.ae2TransferAmount(original);
    }
}
