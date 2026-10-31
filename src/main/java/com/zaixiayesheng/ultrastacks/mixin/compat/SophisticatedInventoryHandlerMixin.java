package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙储存与背包的槽位上限由 {@code InventoryHandler} 报出，两个入口都要抬：{@code getSlotLimit} 报固定上限，{@code getStackLimit} 还要看具体物品。
     *
 * <p>Sophisticated Storage and Backpacks report slot limits from {@code InventoryHandler}, and both
 * entry points have to be raised: {@code getSlotLimit} reports the fixed limit while
 * {@code getStackLimit} also considers the item.
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

    /**
     * {@code getBaseStackLimit(ItemStack)} 内部用 {@code stack.getMaxStackSize()} 当物品侧上限；
     * 存量的脏栈（patch=64、数量=1）在这里仍报 64，存储整理（InventorySorter.placeStack 的
     * getStackLimit 委托链）会把合并后的整组按 64 切。改成 {@link UltraStacksConfig#sortCapacity}，
     * 不再看数量。
     *
     * <p>{@code getBaseStackLimit(ItemStack)} uses {@code stack.getMaxStackSize()} as the item-side
     * limit; legacy dirty stacks (patch=64, count=1) still report 64 there, so the storage sorter
     * (via the getStackLimit delegation used by InventorySorter.placeStack) chops the merged group
     * into 64s. Redirect the second call site (the max computation in the ternary) to
     * {@link UltraStacksConfig#sortCapacity}, which ignores the count.
     */
    @Redirect(
            method = "getBaseStackLimit(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I",
                    ordinal = 1),
            require = 0)
    private int ultrastacks$raiseBaseStackItemLimit(ItemStack stack) {
        return UltraStacksConfig.sortCapacity(stack);
    }

}
