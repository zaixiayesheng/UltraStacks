package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙储存与背包的槽位上限由 {@code InventoryHandler} 报出，两个入口都要抬：{@code getSlotLimit} 报固定上限，{@code getStackLimit} 还要看具体物品。
 *
 * <p>两个都只对 64 与 99 生效，换成 {@link UltraStacksConfig#raiseSophisticatedLimit(int)} 的结果；不抬的话整摞放进去仍会按旧上限被拆开或拒绝。
 *
 * <p>Sophisticated Storage and Backpacks report slot limits from {@code InventoryHandler}, and both
 * entry points have to be raised: {@code getSlotLimit} reports the fixed limit while
 * {@code getStackLimit} also considers the item.
 *
 * <p>Both only change 64 and 99, to the result of
 * {@link UltraStacksConfig#raiseSophisticatedLimit(int)}; without the patch a whole stack is still
 * split or refused on the way in because of the old limit.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler", remap = false)
public abstract class SophisticatedInventoryHandlerMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }

    @Inject(method = "getStackLimit(ILnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"),
            cancellable = true, require = 0)
    private void ultrastacks$raiseStackLimit(int slot, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }
}
