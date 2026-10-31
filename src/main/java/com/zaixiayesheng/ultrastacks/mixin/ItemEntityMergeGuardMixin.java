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
 * 地上的掉落物按配置上限合并。
     *
 * <p>Merges dropped stacks up to the configured limit. Vanilla {@code areMergable} adds the two
 * counts as {@code int}, so two stacks near a billion overflow into a negative number, the
 * sum-below-limit test passes, and the merge is allowed by mistake; the guard adds them as
 * {@code long} first. Vanilla {@code merge} also moves a fixed 64 items per call, which the second
 * hook replaces with the configured limit so a merge can fill the stack in one step.
 */
@Mixin(ItemEntity.class)
public class ItemEntityMergeGuardMixin {

    /**
     * 用 {@code long} 求和来判断两个堆叠放不放得下，避免 int 溢出导致的错误合并。
     *
     * <p>Decides whether two stacks fit together by summing them as {@code long}, avoiding the wrong
     * answer an {@code int} overflow would give. If either side is empty the hook defers to vanilla,
     * whose empty-stack check cannot overflow.
     */
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

    /**
     * 把 {@code merge} 里写死的 64 换成配置上限。
     *
     * <p>Replaces the hard-coded 64 in {@code merge} with the configured limit. With
     * {@code require = 0} the hook is skipped if the constant is missing from the target method,
     * so it cannot break startup.
     */
    @ModifyConstant(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(intValue = 64), require = 0)
    private static int ultrastacks$mergeAmount(int original) {
        return UltraStacksConfig.maxStackSize();
    }
}
