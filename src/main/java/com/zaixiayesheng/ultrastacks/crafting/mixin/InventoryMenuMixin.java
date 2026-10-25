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
 * 背包 2x2 格的批量合成入口。
 *
 * <p>与工作台是同一套逻辑，只有背包区间不同：9..45（槽 0 是产物、1..4 是合成格、5..8 是盔甲、
 * 其余为背包）。
 *
 * <p>Bulk-crafting entry point for the player's 2x2 grid. Same logic as the crafting table, only the
 * inventory range differs: 9..45 (slot 0 is the result, 1..4 the grid, 5..8 armour, the rest the
 * inventory).
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
