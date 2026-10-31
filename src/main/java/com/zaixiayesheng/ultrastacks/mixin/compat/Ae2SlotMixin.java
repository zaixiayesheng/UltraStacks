package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 应用能源的菜单槽位上限。
     *
 * <p>Slot limits of Applied Energistics 2's menu slots.
 */
@Mixin(targets = "appeng.menu.slot.AppEngSlot", remap = false)
public abstract class Ae2SlotMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.ae2SlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"),
            cancellable = true, require = 0)
    private void ultrastacks$raiseStackLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.ae2SlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
