package com.zaixiayesheng.ultrastacks.crafting.mixin;

import com.zaixiayesheng.ultrastacks.crafting.BulkCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the vanilla one-craft-per-round loop for the player's 2x2 grid.
 *
 * <p><b>中文</b> 背包 2x2：玩家 Shift 点击产物槽时一次合完整批，返回空堆叠即可让原版循环停止。
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Shadow
    @Final
    private CraftingContainer craftSlots;

    @Shadow
    @Final
    private ResultContainer resultSlots;

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$bulkCraft(Player player, int slotId, CallbackInfoReturnable<ItemStack> cir) {
        InventoryMenu menu = (InventoryMenu) (Object) this;
        if (slotId != 0) {
            return;
        }
        if (BulkCrafting.tryCraftAll(menu, craftSlots, resultSlots, player, 9, 45)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
