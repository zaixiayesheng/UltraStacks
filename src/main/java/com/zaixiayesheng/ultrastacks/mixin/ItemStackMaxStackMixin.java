package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the configured stack limit when vanilla asks a stack for its maximum size.
 *
 * <p>Two things happen here: items whose vanilla limit is one (potions, buckets, enchanted books)
 * can be raised when their configuration asks for it, and a stack never reports a maximum smaller
 * than the number of items it already carries. The second rule keeps every
 * {@code max - count} subtraction in menu code non-negative after a limit was lowered.
 *
 * <p><b>中文</b> 在 {@link ItemStack#getMaxStackSize()} 返回处套用配置：原版上限为 1 的物品（药水、桶、附魔书等）
 * 可以在配置里单独抬高；同时保证报告的上限永远不小于自身数量，避免上限调低后菜单运算出现负数。
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
