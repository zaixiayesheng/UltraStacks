package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 给目标类补上一个 {@code getSlotLimit(int)} 实现（方法合并），让它报出按配置抬高后的上限。
 *
 * <p>方法里传的是 99，也就是原版的绝对堆叠上限：从 99 抬到配置值，配置没调高时结果仍是 99，不会反而变小。
 *
 * <p>Adds a {@code getSlotLimit(int)} implementation to the target class through method merging so it
 * reports the raised limit.
 *
 * <p>The 99 passed in is the game's absolute stack limit, so it is raised to the configured value and
 * stays 99 when the configured limit is not higher; it never shrinks.
 */
@Mixin(targets = "appeng.util.inv.CarriedItemInventory", remap = false)
public abstract class Ae2CarriedItemInventoryMixin {

    public int getSlotLimit(int slot) {
        return UltraStacksConfig.ae2SlotLimit(99);
    }
}
