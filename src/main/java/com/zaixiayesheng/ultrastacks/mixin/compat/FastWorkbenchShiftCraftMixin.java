package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.crafting.BulkCrafting;
import dev.shadowsoffire.fastbench.util.CraftingInventoryExt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FastWorkbench 的 shift 合成。客户端碰上大堆就直接撤，让服务端一次算完，别在本地空跑几万轮。
 *
 * FastWorkbench's shift-craft. On the client we bail out for huge stacks so the server does the
 * batch once instead of spinning tens of thousands of rounds locally.
 */
@Mixin(targets = "dev.shadowsoffire.fastbench.util.FastBenchUtil", remap = false)
public abstract class FastWorkbenchShiftCraftMixin {

    @Inject(method = "handleShiftCraft(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/inventory/Slot;Ldev/shadowsoffire/fastbench/util/CraftingInventoryExt;Lnet/minecraft/world/inventory/ResultContainer;II)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void ultrastacks$bulkCraft(Player player, AbstractContainerMenu menu, Slot slot,
                                              CraftingInventoryExt inventory, ResultContainer resultSlots,
                                              int startSlot, int endSlot, CallbackInfoReturnable<ItemStack> cir) {
        if (player.level().isClientSide()) {
            // 客户端只发包，合成在服务端。这里不拦掉逐次循环，每轮都会触发合成事件让别的模组扫一遍配方。
            // The client only sends the click; leave the loop running and every round fires
            // a crafting event that other mods answer with a full recipe scan.
            if (BulkCrafting.hasOversizedStacks(inventory)) {
                cir.setReturnValue(ItemStack.EMPTY);
            }
            return;
        }
        if (BulkCrafting.tryCraftAll(menu, inventory, resultSlots, player, startSlot, endSlot)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
