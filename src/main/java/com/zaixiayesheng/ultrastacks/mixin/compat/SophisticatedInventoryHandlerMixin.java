/*
 * 中文：精妙核心 26.1.2 把「每格上限」拆成了 baseSlotLimit（容量倍率）与
 * 每个部件自己的 getSlotLimit（对外报的格子上限）。这里只抬对外报的数，
 * 64 / 99 这种写死值换成配置里的单格上限；正好等于原版值时才算，避免动到
 * 自带更高上限的部件。baseSlotLimit 不动，是因为容量计算会拿它当倍率
 * （baseSlotLimit / 64），一改就会连带把每格容量翻上千倍。
 *
 * English: In sophisticatedcore 26.1.2 the per-slot limit is split between
 * baseSlotLimit (used as a capacity multiplier) and IInventoryPartHandler#getSlotLimit
 * (what the rest of the game reads). Only the reported limit is raised here: hardcoded
 * 64 / 99 become the configured per-slot limit. baseSlotLimit is intentionally left
 * alone, because capacity math multiplies by (baseSlotLimit / 64).
 */
package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler", remap = false)
public class SophisticatedInventoryHandlerMixin {

    @Inject(method = "getInternalSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseReportedSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        int limit = cir.getReturnValueI();
        if (limit == 64 || limit == 99) {
            cir.setReturnValue(UltraStacksConfig.limitCap());
        }
    }
}
