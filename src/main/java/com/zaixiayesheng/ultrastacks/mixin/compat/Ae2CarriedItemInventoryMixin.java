package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 光标上那一格也要跟着配置走。
 *
 * <p>它没有自己的 {@code getSlotLimit}，用的是接口默认值 99，于是拖着一整摞物品点进终端时会被切成 99 个
 * 一堆。这里给目标类补一个真正的重写，直接返回按配置换算过的上限。
 *
 * <p>The carried stack needs to follow the config too.
 *
 * <p>It has no {@code getSlotLimit} of its own and inherits the interface default of 99, so dragging a
 * large stack into a terminal cuts it down to stacks of 99. This adds a real override to the target class
 * that returns the limit computed from the config.
 */
@Mixin(targets = "appeng.util.inv.CarriedItemInventory", remap = false)
public abstract class Ae2CarriedItemInventoryMixin {

    public int getSlotLimit(int slot) {
        return UltraStacksConfig.ae2SlotLimit(99);
    }
}
