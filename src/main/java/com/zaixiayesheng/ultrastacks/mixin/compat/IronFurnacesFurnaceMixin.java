package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 更多熔炉把输出上限写死成 64，三处都换成配置的堆叠上限。
     *
 * <p>Iron Furnaces caps its output at 64 in three places, all replaced with the configured stack limit.
 */
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase", remap = false)
public abstract class IronFurnacesFurnaceMixin {

    @ModifyConstant(method = { "canSmelt", "smeltItemMult", "smeltFactoryItemMult" },
            constant = @Constant(intValue = 64), remap = false, require = 0)
    private int ultrastacks$raiseOutputCap(int original) {
        return UltraStacksConfig.ironFurnaceLimit(original);
    }
}
