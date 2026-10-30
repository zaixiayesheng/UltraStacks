package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.crafting.BulkCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让更多配方一次落地，避免 FastWorkbench 逐次合成的长循环。
 *
 * <p>FastWorkbench 接管 Shift 合成后是一轮一轮地合，产物槽能装下多少就转多少圈；堆叠上限抬到几万之后，
 * 一圈一次的开销积起来就是明显卡顿。这里在它的入口先按整批合成处理，成功就直接返回。
 *
 * <p>FastWorkbench shift-crafts one recipe per iteration; with the raised stack limit that loop runs tens of
 * thousands of times. This runs the whole batch in one go first and returns early when it succeeds.
 */
@Mixin(targets = "dev.shadowsoffire.fastbench.util.FastBenchUtil", remap = false)
public abstract class FastWorkbenchShiftCraftMixin {

    @Inject(method = "handleShiftCraft(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/inventory/Slot;Ldev/shadowsoffire/fastbench/util/CraftingInventoryExt;Lnet/minecraft/world/inventory/ResultContainer;II)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void ultrastacks$bulkCraft(Player player, AbstractContainerMenu menu, Slot slot, Object inventory,
                                       Object resultSlots, int startSlot, int endSlot,
                                       CallbackInfoReturnable<ItemStack> cir) {
        if (!(menu instanceof AbstractCraftingMenu craftingMenu)) {
            return;
        }
        if (BulkCrafting.tryCraftAll(craftingMenu, player, startSlot, endSlot)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
