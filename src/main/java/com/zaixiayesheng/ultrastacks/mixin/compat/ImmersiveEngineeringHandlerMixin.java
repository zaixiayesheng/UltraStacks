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
 * 沉浸工程的物品处理器。
 *
 * <p>它把每格上限写死成 64，外部机器或漏斗往里塞整摞时会被截到 64。这里同时抬高两处：处理器报的上限，
 * 以及插入时按库存上限做的那一次夹取。
 *
 * <p>The item handler of Immersive Engineering. Its per-slot limit is a hardcoded 64, so another machine or
 * a hopper inserting a full stack gets cut down to 64. Both places are raised here: the limit the handler
 * reports and the clamp against the inventory limit that runs while inserting.
 */

@Mixin(targets = "blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler", remap = false)
public class ImmersiveEngineeringHandlerMixin {
    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseHandlerLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.ieSlotLimit(cir.getReturnValue()));
    }

    @Redirect(
            method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE",
                    target = "Lblusunrize/immersiveengineering/common/util/inventory/IIEInventory;getSlotLimit(I)I"),
            require = 0)
    private int ultrastacks$raiseInsertClamp(IIEInventory inventory, int slot) {
        return UltraStacksConfig.ieSlotLimit(inventory.getSlotLimit(slot));
    }
}
