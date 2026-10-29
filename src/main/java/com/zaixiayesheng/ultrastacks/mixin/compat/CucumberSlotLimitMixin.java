package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cucumber 的槽位处理器。
 *
 * <p>神秘农业及其附属机器的槽位都落在它上面，它对每格报的是 64，于是物品叠得再高，机器的输入输出格
 * 也只收 64 个。这里换成配置的堆叠上限，显式设过上限的槽位保持原样。
 *
 * <p>Cucumber's slot handler, which backs the slots of Mystical Agriculture and its addons. It reports 64
 * per slot, so a machine only takes 64 items however high the stack is. The reported value becomes the
 * configured stack size, while a slot that already carries a limit of its own is left alone.
 */
@Mixin(targets = "com.blakebr0.cucumber.inventory.BaseItemStackHandler", remap = false)
public abstract class CucumberSlotLimitMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.mysticalSlotLimit(cir.getReturnValue()));
    }
}
