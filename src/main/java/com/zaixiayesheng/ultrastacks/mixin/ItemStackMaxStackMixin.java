package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 一组物品自己报告的上限不能小于它已经装着的数量。
 *
 * <p>原版菜单用 {@code 上限 - 已有数量} 算剩余空间。当一个堆叠是在「上限更大」的配置下造出来的，
 * 之后配置被调小（或换了存档），这个减法会变成负数：{@code grow(负数)} 让格子里的东西变少、
 * {@code shrink(负数)} 让手上那半变大 —— 表现就是「拆两半分别放进去，一半消失 / 多出一个」。
 *
 * <p>把返回值抬到至少等于当前数量，所有这类减法都会变成非负，既不会丢也不会白送。
 * 这也是 1.20.1 与 1.21.1 共用的核心修复（1.21.1 走组件、1.20.1 走 {@code Item.maxStackSize}，
 * 但本方法的签名与语义相同）。
 *
 * <p><b>EN</b> A stack never reports a maximum smaller than its own count, so menu headroom maths cannot go negative.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMaxStackMixin {

    /**
     * 1.20.1 的 {@code ItemStack.getMaxStackSize()} 走 Forge 的 {@code Item.getMaxStackSize(ItemStack)}
     * 重载（不是原版无参方法，注解处理器看不到），所以在这里统一把「原版上限」换成配置上限，
     * 同时保证返回值不小于当前数量。
     */
    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$neverBelowCount(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        int raised = UltraStacksConfig.scaledMaxForItem(cir.getReturnValue());
        int count = self.getCount();
        cir.setReturnValue(Math.max(raised, count));
    }
}
