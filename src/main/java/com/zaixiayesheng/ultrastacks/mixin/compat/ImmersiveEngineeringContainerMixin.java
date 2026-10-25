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
 * <p>它把内部库存包成 {@code Container}，每格上限报的同样是 64，这里换成配置的堆叠上限。注入用的方法名
 * 是 {@code m_6893_}，也就是 {@code Container.getMaxStackSize} 的 SRG 名——实现类里的这个重写在正式
 * 环境下带的就是这个名字。
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
