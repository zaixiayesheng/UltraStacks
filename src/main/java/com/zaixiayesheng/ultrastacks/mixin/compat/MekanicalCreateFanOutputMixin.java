package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 通用动力的鼓风机产物模拟。
     *
 * <p>The fan output simulation of Mekanical Create caps each slot at 64 while simulating, so a large stack
 * is treated as not fitting; the cap becomes the configured stack size here.
 */
@Mixin(targets = "io.github.langqi99.mekanicalcreate.content.FanOutputCapacity", remap = false)
public class MekanicalCreateFanOutputMixin {

    @ModifyConstant(method = "describe", constant = @Constant(intValue = 64), require = 0, remap = false)
    private static int ultrastacks$raiseFanOutputLimit(int original) {
        return UltraStacksConfig.maxStackSize();
    }
}
