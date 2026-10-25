package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 更多熔炉把输出上限写死成 64，三处都换成配置的堆叠上限。
 *
 * <p>{@code canSmelt} 判断产物放不放得下时拿 64 当上限，{@code smeltItemMult} 与
 * {@code smeltFactoryItemMult} 也用 64 算还能烧几个；不改的话输入格能堆到 65536，输出格却只攒到 64。
 *
 * <p>{@code smelt} 与 {@code factorySmelt} 里同样出现 64，但那里的含义是「一次烧几个」而不是槽位上限，改
 * 它会连熔炼速度一起改掉，所以刻意不碰。只替换 64 与 99 这两种原版尺寸的值，兼容开关关闭时原样返回。
 *
 * <p>Iron Furnaces caps its output at 64 in three places, all replaced with the configured stack limit.
 *
 * <p>{@code canSmelt} uses 64 as the cap when deciding whether the output fits, and {@code smeltItemMult} plus
 * {@code smeltFactoryItemMult} use it to work out how many more items can be smelted; without this the input
 * slot could hold 65536 while the output stopped at 64.
 *
 * <p>The number 64 also appears in {@code smelt} and {@code factorySmelt}, but there it means "how many items
 * per operation" rather than a slot limit, and changing it would change smelting speed, so those are
 * deliberately left alone. Only the two vanilla-sized values, 64 and 99, are replaced, and everything is
 * returned unchanged while the compat switch is off.
 */
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase", remap = false)
public abstract class IronFurnacesFurnaceMixin {

    @ModifyConstant(method = { "canSmelt", "smeltItemMult", "smeltFactoryItemMult" },
            constant = @Constant(intValue = 64), remap = false, require = 0)
    private int ultrastacks$raiseOutputCap(int original) {
        return UltraStacksConfig.ironFurnaceLimit(original);
    }
}
