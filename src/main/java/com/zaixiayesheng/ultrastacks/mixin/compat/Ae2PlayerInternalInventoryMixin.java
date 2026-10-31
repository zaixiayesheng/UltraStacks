package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 玩家背包被应用能源包成 {@code PlayerInternalInventory} 之后也得跟着配置走。
     *
 * <p>The player inventory, once wrapped by Applied Energistics 2 as {@code PlayerInternalInventory}, has to
 * follow the config as well.
 */
@Mixin(targets = "appeng.util.inv.PlayerInternalInventory", remap = false)
public abstract class Ae2PlayerInternalInventoryMixin {

    public int getSlotLimit(int slot) {
        return UltraStacksConfig.ae2SlotLimit(99);
    }
}
