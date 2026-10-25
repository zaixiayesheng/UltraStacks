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
 * Shift 点击铁砧产物时，一次结算付得起的一整批。
 *
 * <p>原版每 Shift 一次只搬一件，搬完还要重算一遍结果；大堆叠上这让玩家点到手软。这里在
 * {@code quickMoveStack} 入口处一次性处理：件数取左槽数量、附加材料够付的件数、经验够付的件数与产物
 * 堆叠上限的最小值，写进背包后统一扣材料、清产物、扣经验。背包放不下就交回原版逐件处理，不吞物品。
 *
 * <p>改名（没有附加材料、也不消耗修复耐久）仍交给原版：整堆改名只扣一次经验，本来就要这个效果。
 *
 * <p>Shift-clicking an anvil result settles a whole affordable batch at once. Vanilla moves a single item
 * per shift-click and recomputes the result afterwards, which is painful on a large stack. Here
 * {@code quickMoveStack} handles it up front: the batch is the smallest of the left slot count, what the
 * additive supply can pay for, what the experience can pay for and the output stack limit; the items are
 * added to the inventory and then the inputs, the result slot and the levels are settled together. When
 * the inventory cannot hold the batch, vanilla takes over item by item rather than losing anything.
 * Renaming, meaning no additive and no repair cost, is still left to vanilla, which charges its
 * experience once for the whole stack as intended.
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
            // 纯改名：整堆只扣一次经验，原版处理得正好，交回给它。
            return;
        }
        int additivePerCraft = anvil.repairItemCountCost > 0 ? anvil.repairItemCountCost : (additive.isEmpty() ? 0 : 1);

        long crafts = input.getCount();
        if (additivePerCraft > 0) {
            crafts = Math.min(crafts, additive.getCount() / additivePerCraft);
        }
        boolean creative = player.getAbilities().instabuild;
        if (!creative) {
            crafts = Math.min(crafts, player.experienceLevel / cost);
        }
        ItemStack template = resultSlot.getItem();
        crafts = Math.min(crafts, template.getMaxStackSize());
        if (crafts <= 1) {
            return;
        }

        ItemStack output = template.copy();
        output.setCount((int) crafts);
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
        anvil.setMaximumCost(0);
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
            } else if (ItemStack.isSameItemSameTags(stack, prototype)) {
                free += Math.max(0, limit - stack.getCount());
            }
        }
        return free;
    }
}
