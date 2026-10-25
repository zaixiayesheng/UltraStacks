package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 更多熔炉的 {@code BlockIronFurnaceTileBase} 在 {@code canSmelt}、{@code smeltItemMult} 与 {@code smeltFactoryItemMult} 里出现常数 64。
 *
 * <p>这三处的 64 换成 {@link UltraStacksConfig#ironFurnaceLimit(int)}；其它方法（包括 {@code smelt}）里的同名常数不在替换范围内，保持原样。
 *
 * <p>More Furnaces' {@code BlockIronFurnaceTileBase} uses the constant 64 in {@code canSmelt},
 * {@code smeltItemMult} and {@code smeltFactoryItemMult}.
 *
 * <p>That 64 is replaced by {@link UltraStacksConfig#ironFurnaceLimit(int)} in those three methods
 * only; the same constant in any other method, {@code smelt} included, is outside the replacement and
 * stays untouched.
 */
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase", remap = false)
public abstract class IronFurnacesFurnaceMixin {

    @ModifyConstant(method = { "canSmelt", "smeltItemMult", "smeltFactoryItemMult" },
            constant = @Constant(intValue = 64), remap = false, require = 0)
    private int ultrastacks$raiseOutputCap(int original) {
        return UltraStacksConfig.ironFurnaceLimit(original);
    }
}
