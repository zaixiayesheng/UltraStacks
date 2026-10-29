package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 应用能源把玩家背包包成的内部库存。
 *
 * <p>这层包装没有自己的 {@code getSlotLimit}，用的是默认的 99，终端里合成出来的东西塞进背包时会被切成
 * 99 个一堆。目标是类，补上的重写会真正生效，返回配置的堆叠上限。
 *
 * <p>The internal inventory AE2 wraps the player inventory in. The wrapper has no {@code getSlotLimit} of
 * its own and inherits a default of 99, so crafted results are split into stacks of 99 when they land in
 * the inventory. The target is a class, so the override added here takes effect and reports the
 * configured stack size.
 */
@Mixin(targets = "appeng.util.inv.PlayerInternalInventory", remap = false)
public abstract class Ae2PlayerInternalInventoryMixin {

    public int getSlotLimit(int slot) {
        return UltraStacksConfig.ae2SlotLimit(99);
    }
}
