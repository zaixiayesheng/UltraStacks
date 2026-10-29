package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 玩家背包被应用能源包成 {@code PlayerInternalInventory} 之后也得跟着配置走。
 *
 * <p>这个包装类没有重写 {@code getSlotLimit}，落到接口默认的 99，于是终端里合成出来的东西往背包里塞时会被
 * 切成 99 个一堆。这里补一个真正的重写，返回按配置换算过的上限。
 *
 * <p>The player inventory, once wrapped by Applied Energistics 2 as {@code PlayerInternalInventory}, has to
 * follow the config as well.
 *
 * <p>The wrapper does not override {@code getSlotLimit} and falls back to the interface default of 99, so
 * crafting results from a terminal are split into stacks of 99 on their way into the inventory. This adds a
 * real override that returns the limit computed from the config.
 */
@Mixin(targets = "appeng.util.inv.PlayerInternalInventory", remap = false)
public abstract class Ae2PlayerInternalInventoryMixin {

    public int getSlotLimit(int slot) {
        return UltraStacksConfig.ae2SlotLimit(99);
    }
}
