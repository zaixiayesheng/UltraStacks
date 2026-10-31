package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 神秘农业及其附属机器走 Cucumber 的槽位处理器，处理器把每格上限默认成 64。
     *
 * <p>Mystical Agriculture and its addons use Cucumber's slot handler, which defaults every slot to 64.
 */
@Mixin(targets = "com.blakebr0.cucumber.inventory.CItemStacksHandler", remap = false)
public abstract class CucumberSlotLimitMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.mysticalSlotLimit(cir.getReturnValue()));
    }
}
