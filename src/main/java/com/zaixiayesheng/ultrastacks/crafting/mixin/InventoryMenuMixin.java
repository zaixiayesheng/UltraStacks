package com.zaixiayesheng.ultrastacks.crafting.mixin;

import com.zaixiayesheng.ultrastacks.crafting.BulkCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 背包 2×2 的批量合成入口，跟工作台一个道理。客户端碰上大堆就撤。
 *
 * Same idea for the 2×2 grid: the client backs off when the grid holds huge stacks.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$bulkCraft(Player player, int slotId, CallbackInfoReturnable<ItemStack> cir) {
        if (slotId != 0) {
            return;
        }
        AbstractCraftingMenu menu = (AbstractCraftingMenu) (Object) this;
        if (player.level().isClientSide()) {
            // 客户端别本地跑：整批由服务端做，本地逐次合成只会把合成事件和配方扫描重复上万遍。
            // Keep the client out of it: the server finishes the batch, while a local pass would repeat
            // the crafting event and its recipe scan tens of thousands of times.
            if (BulkCrafting.hasOversizedInputs(menu.getInputGridSlots())) {
                cir.setReturnValue(ItemStack.EMPTY);
            }
            return;
        }
        if (BulkCrafting.tryCraftAll(menu, player, 9, 45)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
