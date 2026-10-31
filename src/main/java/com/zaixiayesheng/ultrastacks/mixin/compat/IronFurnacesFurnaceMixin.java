package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 更多熔炉在输出判断里写死了 64。
     *
 * <p>Iron Furnaces hardcodes 64 in its output checks. {@code canSmelt} uses
 * {@code Math.min(stack.getMaxStackSize(), 64)} to decide whether the result still fits, and the two batch
 * helpers use {@code 64 - output count} to see how many more can be smelted; all three become the
 * configured stack size. The 64s in {@code smelt} and {@code factorySmelt} stay untouched, since those are
 * the batch multiplier of the multi-smelt furnaces and changing them would change recipe speed.
 */
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase", remap = false)
public abstract class IronFurnacesFurnaceMixin {

    @ModifyConstant(method = { "canSmelt", "smeltItemMult", "smeltFactoryItemMult" },
            constant = @Constant(intValue = 64), remap = false, require = 0)
    private int ultrastacks$raiseOutputCap(int original) {
        return UltraStacksConfig.ironFurnaceLimit(original);
    }
}
