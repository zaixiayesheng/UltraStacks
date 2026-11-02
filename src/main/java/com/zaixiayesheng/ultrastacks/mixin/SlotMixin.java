package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 槽位上限兜底：99 换成配置值；同一种物品时不低于槽里已有的数量，
 * 不然 safeInsert 会按「上限减数量」算出的负数去拆，越拆越乱。
     *
 * Slot limit: swap the 99, and for the same item never go below the current
 * count — otherwise safeInsert splits by a negative number.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseVanillaLimit(CallbackInfoReturnable<Integer> cir) {
        // 零格容器只是占位（神化宝石匣拿 new SimpleContainer(0) 当槽位占位，真正的库存在上限为 1 的
        // 宝石表里）。抬它没有意义，反而让界面以为一格能塞几万个，所以原样返回。
        // 1.20.1 没有 MixinExtras，也没法往 Container 接口的默认方法里注入，这条保护就落在真正读上限的
        // 槽位上。
        if (((Slot) (Object) this).container.getContainerSize() == 0) {
            return;
        }
        // 原版容器的默认上限是 64，部分槽位自己写死 99，所以两个值一起映射。
        int raised = UltraStacksConfig.raiseLegacyDefault(cir.getReturnValue());
        ItemStack current = ((Slot) (Object) this).getItem();
        cir.setReturnValue(current.isEmpty()
                ? raised
                : UltraStacksConfig.honestLimit(raised, current.getCount()));
    }

    /**
     * 带 {@link ItemStack} 参数的重载：Forge 的 {@code safeInsert} 与点击处理走的是这条路。
     * 部分槽位自己重写了无参版本，绕过了上面那条注入，所以这里也把 64 与 99 抬到配置上限。
     *
     * <p>The overload taking an {@link ItemStack}, which is the path Forge's {@code safeInsert} and the
     * click handling use. Slots that override the no-argument version bypass the injection above, so the
     * hardcoded 64 and 99 are raised here as well.
     */
    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$neverBelowContents(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (((Slot) (Object) this).container.getContainerSize() == 0) {
            return;
        }
        int raised = UltraStacksConfig.raiseLegacyDefault(cir.getReturnValue());
        ItemStack current = ((Slot) (Object) this).getItem();
        if (!current.isEmpty() && ItemStack.isSameItemSameTags(current, stack)) {
            raised = UltraStacksConfig.honestLimit(raised, current.getCount());
        }
        cir.setReturnValue(raised);
    }
}
