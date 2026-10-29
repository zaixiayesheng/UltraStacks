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
 * <p>原版 {@code areMergable} 用 int 把两个数量相加再和上限比较：大堆叠相加会溢出成负数，比较结果变成
 * 「装得下」，于是两个其实合并不了的堆叠也会被合并；{@code merge} 又是一次只搬 64 个，配置抬高后合并一件
 * 大堆叠要来回搬上千趟。这里先用 long 相加判断，装得下才把判断交回原版（物品与组件是否相同由原版负责）；
 * 合并量则换成配置的堆叠上限。任一输入为空时不做处理。
 *
 * <p>Merges dropped stacks up to the configured limit.
 *
 * <p>Vanilla {@code areMergable} adds the two counts as int and compares that sum with the limit: adding
 * two large stacks overflows into a negative number, the comparison then says "they fit", and two stacks
 * that cannot actually be merged get merged anyway. {@code merge} also moves only 64 items per call, so
 * with a raised limit merging one large stack takes thousands of round trips. This adds the counts as long
 * first and only hands the decision back to vanilla when the sum fits (whether the items and components
 * match stays vanilla's job); the merge amount becomes the configured stack limit. An empty stack on
 * either side is left alone.
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
