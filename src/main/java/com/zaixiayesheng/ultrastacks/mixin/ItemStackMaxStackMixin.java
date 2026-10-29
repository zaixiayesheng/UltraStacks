package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 {@link ItemStack#getMaxStackSize()} 返回处套用配置。
 *
 * <p>两件事：一是让原版上限为 1 的物品（药水、附魔书、桶）能按各自配置单独抬到高值；二是返回值永远
 * 不小于自身数量——上限被调低之后，菜单代码里的 {@code 上限 - 数量} 会变成负数，{@code split} /
 * {@code shrink} 就会反向操作。
 *
 * <p>Applies the configuration where {@link ItemStack#getMaxStackSize()} returns. Two things happen:
 * items whose vanilla limit is one (potions, enchanted books, buckets) can be raised individually
 * through their own settings, and the result is never smaller than the stack's own count, because a
 * lowered limit would make {@code limit - count} negative in menu code and drive {@code split} or
 * {@code shrink} with a negative amount.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMaxStackMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$applyLimit(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        int original = cir.getReturnValue();
        int limit = Math.max(UltraStacksConfig.limitFor(self, original), self.getCount());
        if (limit != original) {
            cir.setReturnValue(limit);
        }
    }
}
