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
 * 沉浸工程机器界面用的容器包装。
     *
 * <p>The container wrapper Immersive Engineering uses for its machine screens. It exposes the internal
 * inventory as a {@code Container} with the same hardcoded 64 per slot, which becomes the configured stack
 * size. The injected method is named {@code m_6893_}, the SRG name of {@code Container.getMaxStackSize},
 * because that is the name this override carries in production.
 */

@Mixin(targets = "blusunrize.immersiveengineering.common.gui.BlockEntityInventory", remap = false)
public class ImmersiveEngineeringContainerMixin {
    @Inject(method = "m_6893_()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseContainerLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.ieSlotLimit(cir.getReturnValue()));
    }
}
