package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 槽位上限。三层：
 *
 * <ol>
 *   <li>容器返回的原版 99 换成配置上限（有的容器自己重写，光补接口默认方法盖不住，所以在这一层再兜一次）；</li>
 *   <li>本槽的上限不小于它当前装着的数量——堵住「剩余空间 = 上限 - 已有数量」变成负数；
 *       只要装的是同一种物品才抬，不然槽里 1,000 个铁锭会让 16 个一组的鸡蛋也能塞 1,000；</li>
 *   <li>同理处理带 {@link ItemStack} 参数的重载（原版是 {@code min(槽位上限, 物品上限)}）。</li>
 * </ol>
 *
 * <p><b>EN</b> Raises slot limits to the configured value and keeps them at least as large as the stack inside.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseVanillaLimit(CallbackInfoReturnable<Integer> cir) {
        // 1.20.1 原版容器的默认上限是 64（1.21.1 是 99），所以按 64/99 一起映射。
        int raised = UltraStacksConfig.raiseLegacyDefault(cir.getReturnValue());
        ItemStack current = ((Slot) (Object) this).getItem();
        cir.setReturnValue(current.isEmpty() ? raised : Math.max(raised, current.getCount()));
    }

    /**
     * 带 {@link ItemStack} 参数的重载才是插入路径（{@code safeInsert}、{@code doClick}）真正调用的方法。
     * 部分原版槽位（酿造台的燃料槽、药水槽）自己重写了无参版本并直接返回 64/1，绕过了上面那个注入，
     * 所以这里也要把 64/99 抬到配置上限。
     */
    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$neverBelowContents(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int raised = UltraStacksConfig.raiseLegacyDefault(cir.getReturnValue());
        ItemStack current = ((Slot) (Object) this).getItem();
        if (!current.isEmpty() && ItemStack.isSameItemSameTags(current, stack)) {
            raised = Math.max(raised, current.getCount());
        }
        cir.setReturnValue(raised);
    }
}
