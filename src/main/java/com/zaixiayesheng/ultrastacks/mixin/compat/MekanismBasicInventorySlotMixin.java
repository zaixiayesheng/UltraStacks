package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 通用机械的机器槽位 {@code BasicInventorySlot} 由槽对象自己报上限（{@code getLimit(ItemStack)}），不经过原版 {@code Slot}。
     *
 * <p>Mekanism's machine slot {@code BasicInventorySlot} reports its limit from the slot object through
 * {@code getLimit(ItemStack)} instead of going through vanilla {@code Slot}.
 */
@Mixin(targets = "mekanism.common.inventory.slot.BasicInventorySlot", remap = false)
public class MekanismBasicInventorySlotMixin {

    @Inject(method = "getLimit(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true,
            require = 0)
    private void ultrastacks$raiseMekanismLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.raiseMekanismLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
