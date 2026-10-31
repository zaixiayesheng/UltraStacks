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
 * 拖动分摊时每格分到多少。
     *
 * <p>How much of a dragged stack each slot receives. Vanilla uses
 * {@code Mth.floor((float) count / (float) slots.size())}, but a float carries only 24 bits of mantissa
 * and rounds anything above 2^24. Dragging 1,073,741,823 (2^30 - 1) over two slots yields 536,870,912
 * each, 1,073,741,824 in total, one item more than before the drag. Integer division keeps the sum at
 * or below the original count and leaves the remainder on the cursor.
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
