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
 * 接管玩家背包 2x2 合成格的整批合成。
 *
 * <p>Shift 点击产物槽（slotId 0）时一次合完整批并返回空堆叠，原版循环随之停止；其余槽位交还原版。
 *
 * <p>Takes over bulk crafting for the player's 2x2 grid. A shift-click on the result slot (slotId
 * 0) finishes the whole batch and returns an empty stack, which stops the vanilla loop; every
 * other slot is left to vanilla.
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
