package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让精妙储存与精妙背包的槽位放得下整摞配置大小的物品。
     *
 * <p>Lets the slots of Sophisticated Storage and Sophisticated Backpacks hold whole stacks of the configured
 * size.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler", remap = false)
public abstract class SophisticatedInventoryHandlerMixin {

    @Inject(method = "getInternalSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }

    @Inject(method = "getBaseCapacity(Lnet/neoforged/neoforge/transfer/item/ItemResource;)I",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseCapacity(ItemResource resource, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }

    @Inject(method = "getBaseStackLimit(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"),
            cancellable = true, require = 0)
    private void ultrastacks$raiseBaseStackLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }
}
