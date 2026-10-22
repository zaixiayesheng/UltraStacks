package com.zaixiayesheng.ultrastacks.crafting.mixin;

import com.zaixiayesheng.ultrastacks.crafting.BulkCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the vanilla one-craft-per-round loop for the 3x3 crafting table (Minecraft 26.1.2).
 *
 * <p>When the player shift-clicks the result slot we finish the whole batch ourselves and return an
 * empty stack, which is what the vanilla loop treats as "nothing left to move".
 *
 * <p><b>中文</b> 工作台（3x3）：玩家 Shift 点击产物槽时，本模组一次把整批合完并返回空堆叠，
 * 原版的「每轮只合成一次」循环遂立即结束。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$bulkCraft(Player player, int slotId, CallbackInfoReturnable<ItemStack> cir) {
        if (slotId != 0) {
            return;
        }
        AbstractCraftingMenu menu = (AbstractCraftingMenu) (Object) this;
        if (BulkCrafting.tryCraftAll(menu, player, 10, 46)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
