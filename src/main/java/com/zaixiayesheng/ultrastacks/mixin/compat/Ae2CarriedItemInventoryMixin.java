package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 光标上那一格也要跟着配置走。
     *
 * <p>The carried stack needs to follow the config too.
 */
@Mixin(targets = "appeng.util.inv.CarriedItemInventory", remap = false)
public abstract class Ae2CarriedItemInventoryMixin {

    public int getSlotLimit(int slot) {
        return UltraStacksConfig.ae2SlotLimit(99);
    }
}
