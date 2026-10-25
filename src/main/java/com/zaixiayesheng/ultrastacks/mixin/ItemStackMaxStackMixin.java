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
 * <p>原版菜单用「上限减已有数量」算剩余空间。堆叠是在上限更大的配置下造出来的，之后配置调小或者换了
 * 存档，这个减法就会变成负数：{@code grow(负数)} 让格子里的东西变少，{@code shrink(负数)} 让手上那份变大，
 * 表现就是「拆两半分别放进去，一半消失」或者「凭空多出一个」。把返回值抬到至少等于当前数量，
 * 这类减法就都不会是负数，既不丢也不白送。
 *
 * <p>A stack never reports a maximum below its own count. The vanilla menu computes headroom as
 * "limit minus current count"; when a stack was created under a larger limit and the config is later
 * lowered or the save is loaded elsewhere, that subtraction goes negative, {@code grow(negative)}
 * shrinks the slot and {@code shrink(negative)} grows the held stack, so splitting a stack across two
 * slots loses half of it or duplicates an item. Keeping the result at least at the current count makes
 * every such subtraction non-negative.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMaxStackMixin {

    /**
     * {@code ItemStack.getMaxStackSize()} 是 Forge 打过补丁的入口，实际转给
     * {@code Item.getMaxStackSize(ItemStack)}，能拿到堆叠本身，所以在这里统一收口：先由
     * {@link UltraStacksConfig#limitFor} 把原版上限换成当前生效上限，再保证它不小于已装数量。
     *
     * <p>{@code ItemStack.getMaxStackSize()} is the entry Forge patched to forward to
     * {@code Item.getMaxStackSize(ItemStack)} with the stack in hand, so it is the single place to settle
     * this: {@link UltraStacksConfig#limitFor} turns the reported vanilla limit into the one in effect, and
     * the result is then kept at least at the current count.
     */
    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$neverBelowCount(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        int raised = UltraStacksConfig.limitFor(self, cir.getReturnValue());
        int count = self.getCount();
        cir.setReturnValue(Math.max(raised, count));
    }
}
