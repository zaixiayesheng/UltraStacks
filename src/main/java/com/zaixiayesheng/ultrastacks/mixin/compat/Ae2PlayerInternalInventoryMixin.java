package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 应用能源把玩家背包包成的内部库存。
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
