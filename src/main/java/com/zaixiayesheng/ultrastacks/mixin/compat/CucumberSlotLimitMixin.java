package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cucumber 的 {@code BaseItemStackHandler} 是这套槽位实现，{@code getSlotLimit} 报出的上限不经过原版 {@code Slot}；本模组把神秘农业系列的兼容开关挂在这里。
 *
 * <p>只对 64 与 99 生效：换成 {@link UltraStacksConfig#mysticalSlotLimit(int)} 的结果，机器自己指定的更大值保持原样。
 *
 * <p>Cucumber's {@code BaseItemStackHandler} is the slot implementation here, and the limit reported by
 * {@code getSlotLimit} does not go through vanilla {@code Slot}; the compatibility switch for the
 * Mystical Agriculture family hangs off it.
 *
 * <p>Only 64 and 99 are changed, to the result of {@link UltraStacksConfig#mysticalSlotLimit(int)}; a
 * larger value chosen by a machine is kept as is.
 */
@Mixin(targets = "com.blakebr0.cucumber.inventory.BaseItemStackHandler", remap = false)
public abstract class CucumberSlotLimitMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.mysticalSlotLimit(cir.getReturnValue()));
    }
}
