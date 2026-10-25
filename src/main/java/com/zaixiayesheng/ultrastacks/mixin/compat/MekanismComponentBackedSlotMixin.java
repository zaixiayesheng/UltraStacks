package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 通用机械的组件式槽位 {@code ComponentBackedInventorySlot}：上限来自物品自带的容器组件，同样经过 {@code getLimit(ItemStack)}。
 *
 * <p>64 与 99 抬到配置上限，其它更大的数值（例如容器物品自己的容量）原样返回。
 *
 * <p>Mekanism's component-backed slot {@code ComponentBackedInventorySlot} takes its limit from the
 * container component carried by the item, again through {@code getLimit(ItemStack)}.
 *
 * <p>64 and 99 are raised to the configured limit while a larger value, such as the capacity of a
 * container item itself, is returned unchanged.
 */
@Mixin(targets = "mekanism.common.attachments.containers.item.ComponentBackedInventorySlot", remap = false)
public class MekanismComponentBackedSlotMixin {

    @Inject(method = "getLimit(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true,
            require = 0)
    private void ultrastacks$raiseComponentSlotLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.raiseMekanismLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
