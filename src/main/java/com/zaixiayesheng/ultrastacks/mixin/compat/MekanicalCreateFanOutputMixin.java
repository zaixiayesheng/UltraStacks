package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Mekanical Create 在 {@code FanOutputCapacity#describe} 里用常数 64 表示鼓风机产物的单槽容量。
 *
 * <p>把这处常数换成 {@link UltraStacksConfig#maxStackSize()}：这里拿不到具体物品，只能用配置的堆叠上限，而不是按物品缩放的 {@code scaledMax}。
 *
 * <p>Mekanical Create uses the constant 64 in {@code FanOutputCapacity#describe} as the per-slot
 * capacity of a fan output.
 *
 * <p>The constant becomes {@link UltraStacksConfig#maxStackSize()}: no concrete item is available at
 * that point, so the configured stack limit is used instead of the per-item {@code scaledMax}.
 */
@Mixin(targets = "io.github.langqi99.mekanicalcreate.content.FanOutputCapacity", remap = false)
public class MekanicalCreateFanOutputMixin {

    @ModifyConstant(method = "describe", constant = @Constant(intValue = 64), require = 0, remap = false)
    private static int ultrastacks$raiseFanOutputLimit(int original) {
        return UltraStacksConfig.maxStackSize();
    }
}
