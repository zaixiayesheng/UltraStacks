package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ME 接口、样板供应器和一批 AE2 机器共用的通用库存。
     *
 * <p>The generic inventory shared by ME interfaces, pattern providers and a number of AE2 machines.
 */
@Mixin(targets = "appeng.helpers.externalstorage.GenericStackInv", remap = false)
public abstract class Ae2GenericStackInvMixin {

    @ModifyReturnValue(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), require = 0)
    private long ultrastacks$raiseMaxAmount(long original) {
        return UltraStacksConfig.ae2TransferAmount(original);
    }
}
