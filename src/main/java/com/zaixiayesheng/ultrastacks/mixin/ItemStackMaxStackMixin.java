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
    private void ultrastacks$neverBelowCount(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        // 有耐久的物品（工具、武器、护甲）一律按原版 1 走，也不套「不小于自身数量」的兜底。
        // 那条兜底是给 65536 大堆用的，套在工具上就变成自己给自己作证：一堆 2 把的剑声称上限是 2，
        // 于是永远拆不开、拿出来是一叠、用起来一起掉耐久。
        // Damageable items stay at vanilla 1 and skip the "never below own count" floor. That floor
        // exists for huge stacks; on tools it becomes self-justifying, so a stack of two swords claims
        // a limit of two, never splits, and wears down together.
        if (self.isDamageableItem()) {
            return;
        }
        int max = UltraStacksConfig.limitFor(self, cir.getReturnValue());
        if (self.getCount() > max) {
            max = self.getCount();
        }
        if (max != cir.getReturnValue()) {
            cir.setReturnValue(max);
        }
    }
}
