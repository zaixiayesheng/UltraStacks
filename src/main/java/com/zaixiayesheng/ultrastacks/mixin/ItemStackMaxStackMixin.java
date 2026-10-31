package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 getMaxStackSize() 返回处套配置。药水、附魔书、桶这些按各自的配置抬，其余按比例缩。
 *
 * Applies the configured limits where getMaxStackSize() returns.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMaxStackMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$applyLimit(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        // 有耐久的按原版 1，连“不小于自身数量”也不套：那兜底套在工具上就是自我证明，一堆剑永远拆不开。
        // Damageable items stay at 1 and skip the count floor — on tools it self-justifies.
        if (self.isDamageableItem()) {
            return;
        }
        int original = cir.getReturnValue();
        int limit = Math.max(UltraStacksConfig.limitFor(self, original), self.getCount());
        if (limit != original) {
            cir.setReturnValue(limit);
        }
    }
}
