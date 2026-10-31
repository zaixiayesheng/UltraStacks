package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 给目标类补上一个 {@code getSlotLimit(int)} 实现（方法合并），让它报出按配置抬高后的上限。
     *
 * <p>Adds a {@code getSlotLimit(int)} implementation to the target class through method merging so it
 * reports the raised limit.
 */
@Mixin(targets = "appeng.util.inv.CarriedItemInventory", remap = false)
public abstract class Ae2CarriedItemInventoryMixin {

    public int getSlotLimit(int slot) {
        return UltraStacksConfig.ae2SlotLimit(99);
    }
}
