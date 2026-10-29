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
 * 沉浸工程的物品处理器 {@code IEInventoryHandler} 在两处限制单格数量：{@code getSlotLimit} 的返回值，以及 {@code insertItem} 里对 {@code IIEInventory#getSlotLimit} 的那次调用。
 *
 * <p>两处都要抬；只改前者的话，插入时仍会按旧值夹一次，外部机器或漏斗送进来的整摞会被截到旧上限。
 *
 * <p>Immersive Engineering's item handler {@code IEInventoryHandler} limits a slot in two places: the
 * return value of {@code getSlotLimit} and the call to {@code IIEInventory#getSlotLimit} inside
 * {@code insertItem}.
 *
 * <p>Both are raised; changing only the former would still clamp on insert, cutting a whole stack
 * pushed in by another machine or a hopper down to the old limit.
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
