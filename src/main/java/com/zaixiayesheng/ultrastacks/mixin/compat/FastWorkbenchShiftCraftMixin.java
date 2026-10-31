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
            // 客户端只负责发点击包，真正的合成在服务端；逐次循环留在客户端，就是几万次配方扫描。
            // The client only sends the click and the server does the crafting. Leaving the per-round
            // loop on the client means tens of thousands of recipe scans.
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
