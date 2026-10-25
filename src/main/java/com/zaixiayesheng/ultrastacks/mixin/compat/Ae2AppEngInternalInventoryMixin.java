package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 应用能源自己的库存实现：每格上限放在一个 int 数组里，构造时默认填 64。
 *
 * <p>终端里的合成格、接口的配置格都走这套库存，所以在这里抬一次，格子上限就跟着配置一起放大。只替换 64
 * 与 99 这两种原版尺寸的值，模组自己设了别的上限时保持原值；兼容开关关闭时原样返回。
 *
 * <p>Applied Energistics 2's own inventory implementation, which keeps per-slot limits in an int array
 * that starts out filled with 64.
 *
 * <p>The terminal's crafting grid and the interface's configuration slots both go through this inventory,
 * so raising the limit here lets the slot limits follow the config. Only the two vanilla-sized values, 64
 * and 99, are replaced; a different limit set by the mod itself is kept, and everything is returned
 * unchanged while the compat switch is off.
 */
@Mixin(targets = "appeng.util.inv.AppEngInternalInventory", remap = false)
public abstract class Ae2AppEngInternalInventoryMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.ae2SlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
