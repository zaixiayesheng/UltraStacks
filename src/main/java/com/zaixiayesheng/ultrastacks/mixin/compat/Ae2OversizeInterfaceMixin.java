package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ExtendedAE 超大接口的每槽容量单独补一处。
     *
 * <p>A separate hook for the per-slot capacity of ExtendedAE's oversized interface.
 */
@Mixin(targets = "com.glodblock.github.extendedae.common.inventory.OversizeConfigInv", remap = false)
public abstract class Ae2OversizeInterfaceMixin {

    @ModifyReturnValue(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), require = 0)
    private long ultrastacks$raiseOversizeCapacity(long original) {
        return UltraStacksConfig.ae2InterfaceCapacity(original);
    }
}
