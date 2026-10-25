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
 * 接管工作台（3x3）产物槽的 Shift 点击，把「每轮合成一次」的循环换成一次合完整批。
 *
 * <p>注入点在 {@code CraftingMenu#quickMoveStack} 的头部：索引 0 是产物槽，玩家 Shift 点击它时先让
 * {@link com.zaixiayesheng.ultrastacks.crafting.BulkCrafting#tryCraftAll} 尝试整批合成，成功就返回空
 * 堆叠——原版那个 {@code while} 循环正是把空堆叠当作「没东西可搬」而结束，因此不会再逐次合成。返回
 * false 时不设置返回值，原版逻辑照常跑。
 *
 * <p>传进去的 {@code 10} 与 {@code 46} 是该菜单里玩家背包格的起止索引，覆盖主背包与快捷栏共 36 格。
 *
 * <p>Takes over shift-clicks on the result slot of the 3x3 crafting table and replaces the
 * "one craft per round" loop with a single whole-batch craft.
 *
 * <p>The injection point is the head of {@code CraftingMenu#quickMoveStack}: index 0 is the result
 * slot, and when the player shift-clicks it,
 * {@link com.zaixiayesheng.ultrastacks.crafting.BulkCrafting#tryCraftAll} is asked to finish the whole
 * batch, in which case an empty stack is returned — that empty stack is exactly what vanilla's
 * {@code while} loop reads as "nothing left to move", so it stops instead of crafting again and again.
 * When it returns false the return value is left alone and vanilla runs as usual.
 *
 * <p>The {@code 10} and {@code 46} passed in are the first and last-plus-one slot indices of the
 * player's inventory in that menu, covering the main inventory and the hotbar, 36 slots in total.
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
