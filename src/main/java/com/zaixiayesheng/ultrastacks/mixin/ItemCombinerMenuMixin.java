package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 铁砧的 Shift 点击一次结算整批，不必让原版循环跑几千轮。
 *
 * <p>注入点在 {@code ItemCombinerMenu#quickMoveStack} 的头部，只处理 {@link AnvilMenu}、且点的是结果槽
 * 的情况。可结算的件数取四者的最小值：左槽数量、右槽可用量（按每次消耗 {@code repairItemCountCost} 换算，
 * 没有该代价时每次 1 件）、付得起的经验次数（创造模式不限制）、产物的堆叠上限。少于 2 件就返回，交回原版
 * 处理；背包放不下整批时也返回，让玩家自己腾地方，不会把产物丢在地上。
 *
 * <p>改名整段不接管：右槽为空且没有修复代价时直接返回，原版本来就是整堆改名、只扣一次经验。
 *
 * <p>Finishes a whole anvil batch on shift-click instead of letting the vanilla loop run thousands of
 * rounds.
 *
 * <p>The injection point is the head of {@code ItemCombinerMenu#quickMoveStack} and only {@link AnvilMenu}
 * with a click on the result slot is handled. The batch size is the smallest of four numbers: the count in
 * the left slot, the additive supply converted at {@code repairItemCountCost} per operation (one item when
 * there is no such cost), the number of operations the player can pay for by experience (unlimited in
 * creative mode), and the result's stack limit. Fewer than two items is handed back to vanilla, and so is a
 * batch the inventory cannot hold, so the player has to make room instead of having the result dropped on
 * the ground.
 *
 * <p>Renaming is never taken over: with an empty additive slot and no repair cost the method returns and
 * vanilla renames the whole stack for a single charge of experience, as it always did.
 */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$bulkAnvilTake(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object) this instanceof AnvilMenu anvil)) {
            return;
        }
        if (index != anvil.getResultSlot()) {
            return;
        }
        Slot resultSlot = anvil.getSlot(anvil.getResultSlot());
        if (!resultSlot.hasItem()) {
            return;
        }
        int cost = anvil.getCost();
        if (cost <= 0) {
            return;
        }

        ItemStack input = anvil.getSlot(0).getItem();
        if (input.getCount() <= 1) {
            return;
        }
        ItemStack additive = anvil.getSlot(1).getItem();
        if (additive.isEmpty() && anvil.repairItemCountCost == 0) {
            // Pure rename: vanilla charges once for the whole stack, so let vanilla handle it.
            return;
        }
        int additivePerCraft = anvil.repairItemCountCost > 0 ? anvil.repairItemCountCost : (additive.isEmpty() ? 0 : 1);

        long crafts = input.getCount();
        if (additivePerCraft > 0) {
            crafts = Math.min(crafts, additive.getCount() / additivePerCraft);
        }
        boolean creative = player.isCreative();
        if (!creative) {
            crafts = Math.min(crafts, player.experienceLevel / cost);
        }
        ItemStack template = resultSlot.getItem();
        crafts = Math.min(crafts, template.getMaxStackSize());
        if (crafts <= 1) {
            return;
        }

        ItemStack output = template.copyWithCount((int) crafts);
        if (freeSpace(player, output) < crafts) {
            return;
        }

        player.getInventory().add(output);
        input.shrink((int) crafts);
        if (additivePerCraft > 0) {
            additive.shrink((int) (additivePerCraft * crafts));
        }
        anvil.repairItemCountCost = 0;
        resultSlot.set(ItemStack.EMPTY);
        anvil.setCost(0);
        if (!creative) {
            player.giveExperienceLevels(-cost * (int) crafts);
        }
        anvil.slotsChanged(anvil.getSlot(0).container);
        cir.setReturnValue(ItemStack.EMPTY);
    }

    private static long freeSpace(Player player, ItemStack prototype) {
        Inventory inventory = player.getInventory();
        long free = 0L;
        int limit = Math.max(1, prototype.getMaxStackSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                free += limit;
            } else if (ItemStack.isSameItemSameComponents(stack, prototype)) {
                free += Math.max(0, limit - stack.getCount());
            }
        }
        return free;
    }
}
