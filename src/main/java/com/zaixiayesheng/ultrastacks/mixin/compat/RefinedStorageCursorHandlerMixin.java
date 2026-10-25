package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 精致存储的光标处理器 {@code CursorItemHandler} 用 {@code getSlotLimit} 报单格容量，不走原版 {@code Slot}。
 *
 * <p>只把 64 与 99 换成 {@link UltraStacksConfig#refinedStorageSlotLimit(int)}，其它值原样返回，免得把网络里已有的更大容量压小。
 *
 * <p>Refined Storage's cursor handler {@code CursorItemHandler} reports a per-slot capacity through
 * {@code getSlotLimit} instead of vanilla {@code Slot}.
 *
 * <p>Only 64 and 99 become {@link UltraStacksConfig#refinedStorageSlotLimit(int)}; anything else is
 * returned unchanged so a larger capacity already present in the network is not lowered.
 */
@Mixin(targets = "com.refinedmods.refinedstorage.neoforge.grid.strategy.CursorItemHandler", remap = false)
public abstract class RefinedStorageCursorHandlerMixin {

    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"))
    private int ultrastacks$raiseCursorLimit(int original) {
        return UltraStacksConfig.refinedStorageSlotLimit(original);
    }
}
