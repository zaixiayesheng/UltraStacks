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
 * Drag-splitting a stack across several slots divides the count by the number of slots.
 *
 * <p>Vanilla does that in {@code float}: {@code Mth.floor((float) count / (float) slots)}. A float only has
 * 24 bits of mantissa, so any count above 16,777,216 is rounded to a multiple of the float's step. At
 * 1,073,741,823 (2^30 - 1) the value rounds up to 2^30, the two halves become 536,870,912 each and the
 * player ends up with 1,073,741,824 items: one extra item per drag. Doing the division in {@code int}
 * keeps the sum equal to the original count.
 *
 * <p><b>中文</b> 拖拽分摊改用整数除法；原版用 float，超过 2^24 会向上取整多出一个物品。
 */
@Mixin(AbstractContainerMenu.class)
public abstract class QuickCraftSplitMixin {

    @Inject(method = "getQuickCraftPlaceCount", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$integerSplit(Set<Slot> slots, int type, ItemStack stack,
                                                CallbackInfoReturnable<Integer> cir) {
        if (type == 0 && !slots.isEmpty()) {
            cir.setReturnValue(stack.getCount() / slots.size());
        }
    }
}
