package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙储存与精妙背包的槽位上限。
     *
 * <p>The slot limits of Sophisticated Storage and Sophisticated Backpacks. Storage slots report 64 and
 * upgrade slots report 99, so a bigger stack gets split, refused or silently cut whether the player or a
 * hopper moves it in. Both the reported slot limit and the per-slot stack limit are raised here, without
 * touching the item itself.
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

    @Inject(method = "getBaseStackLimit(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"),
            cancellable = true, require = 0)
    private void ultrastacks$raiseBaseStackLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }
}
