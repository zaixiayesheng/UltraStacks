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
        // 资源是具体物品时看一眼它有没有耐久，有就还是 1 个一格。
        // When the resource is a concrete item, keep damageable ones at one per slot.
        if (resource instanceof ItemResource itemResource && itemResource.toStack(1).isDamageableItem()) {
            cir.setReturnValue(1);
            return;
        }
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }

    @Inject(method = "getBaseStackLimit(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"),
            cancellable = true, require = 0)
    private void ultrastacks$raiseBaseStackLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        // 这个方法把物品上限按「槽位上限 / 64」放大，抬过上限之后一把工具会被算成上千个。
        // This method scales the item limit by slotLimit/64, which turns a one-per-slot tool into hundreds.
        if (stack.isDamageableItem()) {
            cir.setReturnValue(1);
            return;
        }
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }
}
