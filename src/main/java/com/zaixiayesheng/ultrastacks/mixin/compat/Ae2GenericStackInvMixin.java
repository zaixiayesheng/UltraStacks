package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ME 接口、样板供应器和一批 AE2 机器共用的通用库存。
 *
 * <p>{@code getMaxAmount} 决定这类库存一次能搬动多少物品，返回 99：堆叠上限调大之后，接口的搬运量就卡在
 * 99 上，一格明明装得下 65536 个，传输速度却跟原来一样。这里把 64 与 99 这两种原版尺寸的值换成「配置上限
 * 的十六分之一，且不小于原值」，既不比原版尺寸时更慢，也不会一次搬走几万个。
 *
 * <p>The generic inventory shared by ME interfaces, pattern providers and a number of AE2 machines.
 *
 * <p>{@code getMaxAmount} decides how many items such an inventory moves at a time and returns 99, so after
 * the stack limit is raised the interface's throughput is still stuck at 99: a slot holds 65536 items while
 * transfers run at the old rate. The two vanilla-sized values, 64 and 99, are replaced here with "a
 * sixteenth of the configured stack limit, never below the original", which is neither slower than the
 * vanilla size nor able to move tens of thousands of items in one go.
 */
@Mixin(targets = "appeng.helpers.externalstorage.GenericStackInv", remap = false)
public abstract class Ae2GenericStackInvMixin {

    @ModifyReturnValue(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), require = 0)
    private long ultrastacks$raiseMaxAmount(long original) {
        return UltraStacksConfig.ae2TransferAmount(original);
    }
}
