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
 * <p>Merges dropped stacks up to the configured limit.
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
