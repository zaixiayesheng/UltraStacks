package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 地上掉落物按配置上限合并。
     *
 * <p>Merging dropped items up to the configured limit. Vanilla {@code areMergable} adds the two counts
 * as int, which overflows to a negative number on large stacks and reports "fits"; {@code merge} then
 * moves 64 at a time. The guard adds the counts as a long first, and the merge amount becomes the
 * configured stack size.
 */
@Mixin(ItemEntity.class)
public class ItemEntityMergeGuardMixin {

    @Inject(method = "areMergable(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$guardMerge(ItemStack destination, ItemStack origin,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (destination.isEmpty() || origin.isEmpty()) {
            return;
        }
        long sum = (long) destination.getCount() + origin.getCount();
        if (sum > origin.getMaxStackSize()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyConstant(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(intValue = 64), require = 0)
    private static int ultrastacks$mergeAmount(int original) {
        return UltraStacksConfig.maxStackSize();
    }
}
