package com.zaixiayesheng.ultrastacks.mixin.compat;

import appeng.api.stacks.AEKey;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ExtendedAE 的超大接口。
     *
 * <p>ExtendedAE's oversized interface implements {@code getMaxAmount} itself instead of going through the
 * parent, so the generic {@code GenericStackInv} patch never reaches it and the capacity is handled here
 * instead: lifted to a sixteenth of the configured stack size and capped at the full stack size.
 */
@Mixin(targets = "com.glodblock.github.extendedae.common.inventory.OversizeConfigInv", remap = false)
public abstract class Ae2OversizeInterfaceMixin {

    @Inject(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseOversizeCapacity(AEKey key, CallbackInfoReturnable<Long> cir) {
        long original = cir.getReturnValueJ();
        long raised = UltraStacksConfig.ae2InterfaceCapacity(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
