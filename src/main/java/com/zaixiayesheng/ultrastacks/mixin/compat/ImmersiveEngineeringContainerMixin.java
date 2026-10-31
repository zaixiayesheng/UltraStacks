package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 沉浸工程的机器界面把内部库存包进这个容器，单格上限由无参的 {@code getMaxStackSize} 决定，不经过原版 {@code Slot}。
     *
 * <p>Immersive Engineering wraps a machine's inventory in this container for its GUI, and the per-slot
 * limit comes from the no-argument {@code getMaxStackSize} instead of vanilla {@code Slot}.
 */

@Mixin(targets = "blusunrize.immersiveengineering.common.gui.BlockEntityInventory", remap = false)
public class ImmersiveEngineeringContainerMixin {
    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseContainerLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.ieSlotLimit(cir.getReturnValue()));
    }
}
