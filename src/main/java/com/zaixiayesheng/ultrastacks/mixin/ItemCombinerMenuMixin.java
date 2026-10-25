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
 * 铁砧产物被 Shift 点击时，一次结算付得起的整批，而不是只搬一份产物。
 *
 * <p>只在 {@code AnvilMenu} 的产物槽生效。批量上限依次取左槽数量、右槽能提供的次数、可支付的经验
 * （创造模式不限）与产物自身的堆叠上限；纯改名（右槽为空且 {@code repairItemCountCost} 为 0）
 * 交还原版，因为原版对整堆只扣一次经验。结算成功后扣材料、扣经验、清空产物槽与花费，并返回空堆叠
 * 让原版不再搬第二次。
 *
 * <p>Settles the whole affordable batch at once when the anvil result is shift-clicked, instead of
 * moving a single result. It only applies to the result slot of an {@code AnvilMenu}. The batch is
 * capped in turn by the left slot count, the number of operations the additive slot can supply,
 * the affordable experience (unlimited in creative) and the result's own stack limit. A pure
 * rename (empty additive slot and {@code repairItemCountCost} of zero) is left to vanilla, which
 * charges experience once for the whole stack. On success the inputs, experience and cost are
 * charged, the result slot is cleared, and an empty stack is returned so vanilla cannot move a
 * second result.
 */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    /**
     * 接管铁砧产物槽的 Shift 点击：算出批量、确认背包放得下、结算材料与经验。
     *
     * <p>批量不超过 1 时直接返回，把这一次点击让给原版，免得普通的一次取一件也走批量路径。
     * 右槽作为修复材料时，一次操作扣 {@code repairItemCountCost} 件，其余情况每次扣 1 件。
     *
     * <p>Takes over the shift-click on the anvil result slot: compute the batch, confirm the inventory
     * has room, then settle the materials and experience. A batch of at most one returns immediately so
     * the ordinary take-one path stays vanilla. While the additive slot serves as repair material, one
     * operation costs {@code repairItemCountCost} items from it and one item otherwise.
     */
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
            // 纯改名：原版对整堆只扣一次经验，这一条留给原版处理。
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
        anvil.setMaximumCost(0);
        if (!creative) {
            player.giveExperienceLevels(-cost * (int) crafts);
        }
        anvil.slotsChanged(anvil.getSlot(0).container);
        cir.setReturnValue(ItemStack.EMPTY);
    }

    /**
     * 背包还能装下多少个这种物品：空格按物品自身的上限计，同类堆叠按剩余空间计。
     *
     * <p>只用来确认整批产物能一次性进背包——装不下就放弃这次批量，交还原版逐件处理。
     *
     * <p>How many more of this item the inventory can take: empty slots count the item's own limit and
     * matching stacks count their remaining room. It only checks that the whole batch fits into the
     * inventory; if it does not, the batch is abandoned and vanilla handles the result one at a time.
     */
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
