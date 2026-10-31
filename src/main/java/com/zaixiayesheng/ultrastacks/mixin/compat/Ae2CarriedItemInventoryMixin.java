package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 光标上那一格。
     *
 * <p>The carried-item slot. It has no {@code getSlotLimit} of its own and inherits a default of 99, so
 * dragging a full stack into a terminal cuts it down. The target is a class rather than an interface,
 * so adding the method here does override the default.
 */
@Mixin(targets = "appeng.util.inv.CarriedItemInventory", remap = false)
public abstract class Ae2CarriedItemInventoryMixin {

    public int getSlotLimit(int slot) {
        return UltraStacksConfig.ae2SlotLimit(99);
    }
}
