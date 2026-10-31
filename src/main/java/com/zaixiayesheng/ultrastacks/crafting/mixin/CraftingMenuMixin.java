package com.zaixiayesheng.ultrastacks.crafting.mixin;

import com.zaixiayesheng.ultrastacks.crafting.BulkCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 工作台的批量合成入口。客户端碰上大堆就撤，别跟着服务端空跑。
 *
 * Bulk-craft entry point for the crafting table. Backs off on the client when the grid holds
 * huge stacks.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Shadow
    @Final
    private CraftingContainer craftSlots;

    @Shadow
    @Final
    private ResultContainer resultSlots;

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$bulkCraft(Player player, int slotId, CallbackInfoReturnable<ItemStack> cir) {
        CraftingMenu menu = (CraftingMenu) (Object) this;
        if (slotId != 0) {
            return;
        }
        if (player.level().isClientSide()) {
            // 客户端别本地跑：整批由服务端做，本地逐次合成只会把合成事件和配方扫描重复上万遍。
            // Keep the client out of it: the server finishes the batch, while a local pass would repeat
            // the crafting event and its recipe scan tens of thousands of times.
            if (BulkCrafting.hasOversizedStacks(craftSlots)) {
                cir.setReturnValue(ItemStack.EMPTY);
            }
            return;
        }
        if (BulkCrafting.tryCraftAll(menu, craftSlots, resultSlots, player, 10, 46)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
