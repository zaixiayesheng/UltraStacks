package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ExtendedAE 超大接口的每槽容量单独补一处。
 *
 * <p>它把库存换成了自己的 {@code OversizeConfigInv} 并重写了 {@code getMaxAmount}，不走父类的
 * {@code GenericStackInv}，所以通用的那层补丁打不进来。这里按接口容量的同一套规则处理：下限是配置上限的
 * 十六分之一，上限是配置上限本身——低于下限的抬上去，高于配置上限的压回来。兼容开关关闭时原样返回。
 *
 * <p>A separate hook for the per-slot capacity of ExtendedAE's oversized interface.
 *
 * <p>It swaps in its own {@code OversizeConfigInv} inventory and overrides {@code getMaxAmount} instead of
 * going through the parent {@code GenericStackInv}, so the generic patch never reaches it. The same rule as
 * the interface capacity is applied here: the floor is a sixteenth of the configured stack limit and the
 * ceiling is the configured stack limit itself.
 */
@Mixin(targets = "com.glodblock.github.extendedae.common.inventory.OversizeConfigInv", remap = false)
public abstract class Ae2OversizeInterfaceMixin {

    @ModifyReturnValue(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), require = 0)
    private long ultrastacks$raiseOversizeCapacity(long original) {
        return UltraStacksConfig.ae2InterfaceCapacity(original);
    }
}
