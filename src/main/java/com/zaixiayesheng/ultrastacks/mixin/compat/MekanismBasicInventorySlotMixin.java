package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 通用机械的机器槽位上限。
     *
 * <p>The per-slot limit of Mekanism machines. {@code BasicInventorySlot#getLimit} reports 99 for most
 * slots, so a raised config still only allows 99 items per machine slot. The 64 and 99 are replaced with
 * the configured stack size, while a slot already set to another value (an output slot holding one, for
 * instance) keeps it.
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
