package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拖拽把一堆物品分摊到多个槽位时，用整数除法算每格放几个。
 *
 * <p>原版在 {@code getQuickCraftPlaceCount} 里用 float 算：{@code Mth.floor((float) count / (float) slots)}。
 * float 只有 24 位尾数，超过 16777216 的数量会被舍入到浮点的步长上。以 1073741823（2^30 - 1）分到两格
 * 为例，数量先被舍入成 2^30，每格算出 536870912，两格合计 1073741824，比原来多出一个——每拖一次多一个。
 * 改成 int 除法后，各格之和不会超过原数量。
 *
 * <p>Splits a dragged stack across slots with integer division.
 *
 * <p>Vanilla computes it in {@code getQuickCraftPlaceCount} with floats:
 * {@code Mth.floor((float) count / (float) slots)}. A float keeps only 24 mantissa bits, so any count above
 * 16777216 is rounded to the float's step. Splitting 1073741823 (2^30 - 1) over two slots rounds the count
 * up to 2^30, gives 536870912 per slot and leaves the player holding 1073741824 — one extra item per drag.
 * With integer division the per-slot amounts never add up to more than the original count.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class QuickCraftSplitMixin {

    @Inject(method = "getQuickCraftPlaceCount", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$integerSplit(int slotsSize, int type, ItemStack stack,
                                                 CallbackInfoReturnable<Integer> cir) {
        if (type == 0 && slotsSize > 0) {
            cir.setReturnValue(stack.getCount() / slotsSize);
        }
    }
}
