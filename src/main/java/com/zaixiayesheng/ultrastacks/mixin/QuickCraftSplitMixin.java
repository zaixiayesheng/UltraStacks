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
 * <p>Splits a dragged stack across slots with integer division.
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
