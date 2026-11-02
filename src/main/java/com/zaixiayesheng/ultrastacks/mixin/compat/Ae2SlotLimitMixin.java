package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE 的槽位上限写死在 {@code InternalInventory.getSlotLimit} 里（合成格、合成产物、副手都是 64/99），
 * 这里统一抬到配置值，让这三类格子能放下超过 64 的堆叠。
 *
 * <p>只管「格子能放多少」，不碰任何绘制：AE 终端里的数字仍由 AE 自己画。
 *
 * <p>AE hard-codes slot limits at 64/99 for the crafting grid, the crafting result and the offhand.
 * Raising them here lets those cells hold oversized stacks. Display is deliberately untouched — AE keeps
 * drawing its own numbers inside the terminal.
 *
 * <p>1.20.1 的 AE2 jar 里 {@code getMaxStackSize()} 带的是 SRG 名 {@code m_6641_}。
 */
@Mixin(targets = "appeng.menu.slot.AppEngSlot", remap = false)
public class Ae2SlotLimitMixin {

    @Inject(method = "m_6641_()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        if (original == 64 || original == 99) {
            cir.setReturnValue(Math.max(original, UltraStacksConfig.limitCap()));
        }
    }
}
