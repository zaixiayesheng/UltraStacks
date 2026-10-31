package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * 拖拽均分改用整数除法，避免 float 舍入凭空多出物品。
     *
 * <p>Uses integer division for drag-splitting so float rounding cannot create items. Vanilla
 * computes the per-slot amount as {@code Mth.floor((float) count / (float) slots.size())}. A float
 * has only 24 bits of mantissa, so a count above 16777216 is rounded to a multiple of the float's
 * step: 1073741823 first becomes 2^30, splitting it into two slots yields 536870912 each, and the
 * player ends up with one item more than before, duplicating an item per drag. Integer division
 * keeps the sum of the slots within the original count.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class QuickCraftSplitMixin {

    /**
     * 只接管「平均分配」这一种拖拽（{@code type == 0}），且目标槽集合非空；其他拖拽类型原样返回。
     *
     * <p>Only the even-distribution drag ({@code type == 0}) with a non-empty slot set is taken over;
     * every other drag type is left to vanilla.
     */
    @Inject(method = "getQuickCraftPlaceCount", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$integerSplit(Set<Slot> slots, int type, ItemStack stack,
                                                CallbackInfoReturnable<Integer> cir) {
        if (type == 0 && !slots.isEmpty()) {
            cir.setReturnValue(stack.getCount() / slots.size());
        }
    }
}
