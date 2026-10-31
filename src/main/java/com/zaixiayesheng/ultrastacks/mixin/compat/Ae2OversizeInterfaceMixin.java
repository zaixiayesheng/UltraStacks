package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ExtendedAE 的超大接口用自己的 {@code OversizeConfigInv} 算容量，父类 {@code GenericStackInv} 上的那处注入到不了这里，所以单独补一层。
     *
 * <p>ExtendedAE's oversized interface computes its capacity in its own {@code OversizeConfigInv}, which
 * the hook on the parent {@code GenericStackInv} cannot reach, so a second hook is added here.
 */
@Mixin(targets = "com.glodblock.github.extendedae.common.inventory.OversizeConfigInv", remap = false)
public abstract class Ae2OversizeInterfaceMixin {

    @ModifyReturnValue(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), require = 0)
    private long ultrastacks$raiseOversizeCapacity(long original) {
        return UltraStacksConfig.ae2InterfaceCapacity(original);
    }
}
