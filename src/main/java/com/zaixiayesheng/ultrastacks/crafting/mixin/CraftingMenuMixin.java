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
 * 工作台（3x3）的批量合成入口。
 *
 * <p>玩家 Shift 点击产物槽（{@code slotId == 0}）时先交给 {@link BulkCrafting}：整批合完就返回空堆叠，
 * 原版「每轮只合成一次」的循环拿到空堆叠后立即结束。背包区间是 10..46。
 *
 * <p>Bulk-crafting entry point for the 3x3 crafting table. On a shift-click of the result slot
 * ({@code slotId == 0}) it hands the click to {@link BulkCrafting} first; when the whole batch is
 * done it returns an empty stack, which is what the vanilla one-craft-per-round loop reads as
 * "nothing left to move". The inventory range is 10..46.
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
