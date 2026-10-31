package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AE2 的通用库存 {@code GenericStackInv} 对每个槽位按 key 报一个容量，{@code getMaxAmount} 就是它。
     *
 * <p>AE2's generic inventory {@code GenericStackInv} reports a capacity per slot for a given key through
 * {@code getMaxAmount}.
 */
@Mixin(targets = "appeng.helpers.externalstorage.GenericStackInv", remap = false)
public abstract class Ae2GenericStackInvMixin {

    @ModifyReturnValue(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), require = 0)
    private long ultrastacks$raiseMaxAmount(long original) {
        return UltraStacksConfig.ae2TransferAmount(original);
    }
}
