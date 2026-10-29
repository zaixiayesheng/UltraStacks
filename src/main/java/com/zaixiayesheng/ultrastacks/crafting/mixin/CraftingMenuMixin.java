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
 * 接管工作台（3x3）的整批合成。
 *
 * <p>玩家 Shift 点击产物槽时整批合完并返回空堆叠：{@code quickMoveStack} 的 slotId 0 就是产物槽，
 * 原版「每轮只合成一次」的循环把空堆叠当成「没有东西可搬」，随即结束。其它槽位的点击交还原版。
 *
 * <p>Takes over bulk crafting for the 3x3 crafting table. When the player shift-clicks the result
 * slot (slot 0 of {@code quickMoveStack}) the whole batch is finished and an empty stack is
 * returned; the vanilla one-craft-per-round loop reads that as "nothing left to move" and stops.
 * Clicks on any other slot go to vanilla untouched.
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
        if (BulkCrafting.tryCraftAll(menu, craftSlots, resultSlots, player, 10, 46)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
