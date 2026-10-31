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
 * 铁砧产物被 Shift 点击时，一次把付得起的整批结算掉。
 * 上限取左槽数量、右槽能提供的次数、付得起的经验和产物自己的上限；纯改名交还原版。
     *
 * Shift-clicking an anvil result settles the affordable batch in one go, capped by
 * the left slot, the additive slot, experience and the result's own limit. Renames go to vanilla.
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
