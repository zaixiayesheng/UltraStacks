package com.zaixiayesheng.ultrastacks.crafting;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.EventHooks;

/**
 * 合成模块：一次 Shift 点击把整批材料合完。
 *
 * <p>原版每合一次就走一遍 {@code quickMoveStack}：重新匹配配方、扣一次料、塞一次产物。一格 65536 个
 * 材料就要在一 tick 内跑 65536 轮，客户端会卡死，服务端也可能超时。这里先算「材料够合几次」与
 * 「背包放得下几次」的较小值，一次扣料，再把产物按单堆上限分块塞回背包。
 *
 * <p>Finishes a whole batch in one shift-click. Vanilla runs a full {@code quickMoveStack} pass per
 * craft: match the recipe again, consume once, push the output once. A slot of 65536 materials
 * would mean 65536 passes inside one tick, which freezes the client and can time the server out.
 * Instead the affordable craft count is computed up front as the smaller of "the materials allow"
 * and "the inventory has room for", materials are consumed once, and the output is inserted in
 * max-stack-sized chunks.
 */
public final class BulkCrafting {

    private BulkCrafting() {
    }

    /**
     * 两个开关都打开时才接管合成：模块总开关，以及模块内部的批量子开关。
     *
     * <p>Bulk crafting runs only when both switches are on: the module switch and the bulk sub-switch
     * inside it.
     */
    public static boolean enabled() {
        return UltraStacksConfig.craftingModule() && UltraStacksConfig.bulkCrafting();
    }

    /**
     * 尝试把产物槽后面的整批材料一次合完。
     *
     * <p>先从产物槽取本次产物并重算配方余料（桶、瓶子这类），再取各非空格的最小数量作为材料上限，
     * 用背包空位换算成次数上限，实际次数取两者较小值。扣料后把余料按单堆上限分块放回原格或背包，
     * 放不下就掉在地上；产物同样分块插入，多出来的部分进背包或掉落。最后补上原版一次合成会做的记账：
     * 物品统计、已用配方、合成事件与槽位刷新。
     *
     * <p>Tries to finish the batch behind the result slot in one go.
     *
     * <p>The result is read from the result slot and the recipe remainders (buckets, bottles and the
     * like) are recomputed. The minimum count over the non-empty crafting slots bounds the craft count
     * from the input side, the free space in the inventory bounds it from the output side, and the
     * smaller value wins. Materials are consumed once; remainders go back into their own slot or the
     * inventory in max-stack-sized chunks and are dropped when nothing has room. The output is
     * inserted the same way. Finally the bookkeeping a vanilla craft would do is performed: item
     * statistics, used recipes, the crafting event and a slot refresh.
     *
     * @param menu 当前菜单，背包槽位按索引从中取出。The menu whose inventory slots are indexed.
     * @param craftSlots 合成格。The crafting grid.
     * @param resultSlots 产物槽；已用配方也在这里读。The result slots, where the used recipe is read from.
     * @param player 参与合成的玩家，负责背包插入与掉落。The player who receives the output.
     * @param inventoryStart 背包槽位的起始索引。First inventory slot index.
     * @param inventoryEnd 背包槽位的结束索引，不含。End index of the inventory slots, exclusive.
     * @return 成功接管本次点击返回 {@code true}，调用方应返回空栈让原版循环停下；返回 {@code false} 时交还原版。{@code true} when the click was handled, so the caller returns an empty stack and the vanilla loop stops; {@code false} lets vanilla handle it.
     */
    public static boolean tryCraftAll(AbstractContainerMenu menu, CraftingContainer craftSlots,
                                      ResultContainer resultSlots, Player player, int inventoryStart, int inventoryEnd) {
        if (!enabled()) {
            return false;
        }

        ItemStack result = resultSlots.getItem(0);
        if (result.isEmpty() || result.getCount() <= 0) {
            return false;
        }

        // 配方余料（桶、瓶子之类）在这里重算，扣料之后按份数还回去。
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(9, ItemStack.EMPTY);
        if (resultSlots instanceof RecipeCraftingHolder holder) {
            RecipeHolder<?> usedRecipe = holder.getRecipeUsed();
            if (usedRecipe != null && usedRecipe.value() instanceof CraftingRecipe craftingRecipe) {
                CraftingInput input = craftSlots.asCraftInput();
                NonNullList<ItemStack> computed = craftingRecipe.getRemainingItems(input);
                for (int i = 0; i < Math.min(computed.size(), remainingItems.size()); i++) {
                    remainingItems.set(i, computed.get(i));
                }
            }
        }

        int limitedByInput = Integer.MAX_VALUE;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (!stack.isEmpty()) {
                limitedByInput = Math.min(limitedByInput, stack.getCount());
            }
        }
        if (limitedByInput == Integer.MAX_VALUE) {
            return false;
        }

        int perCraft = Math.max(1, result.getCount());
        long freeSpace = countFreeSpace(menu, result, inventoryStart, inventoryEnd);
        int crafts = CraftingMath.capBySpace(limitedByInput, freeSpace, perCraft);
        if (crafts <= 0) {
            return false;
        }

        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            if (!craftSlots.getItem(i).isEmpty()) {
                craftSlots.removeItem(i, crafts);
            }
        }

        for (int i = 0; i < remainingItems.size() && i < craftSlots.getContainerSize(); i++) {
            ItemStack rem = remainingItems.get(i);
            if (rem.isEmpty()) {
                continue;
            }
            long total = (long) rem.getCount() * crafts;
            while (total > 0) {
                int chunk = CraftingMath.nextChunk(total, rem.getMaxStackSize());
                ItemStack gave = rem.copyWithCount(chunk);
                ItemStack current = craftSlots.getItem(i);
                if (current.isEmpty()) {
                    craftSlots.setItem(i, gave);
                } else if (ItemStack.isSameItemSameComponents(current, gave)) {
                    int room = Math.max(0, current.getMaxStackSize() - current.getCount());
                    if (room >= chunk) {
                        current.grow(chunk);
                    } else {
                        current.grow(room);
                        gave.shrink(room);
                        if (!player.getInventory().add(gave)) {
                            player.drop(gave, false);
                        }
                    }
                } else if (!player.getInventory().add(gave)) {
                    player.drop(gave, false);
                }
                total -= chunk;
            }
        }

        long remainingOutput = (long) crafts * perCraft;
        long produced = remainingOutput;
        while (remainingOutput > 0) {
            int chunk = CraftingMath.nextChunk(remainingOutput, result.getMaxStackSize());
            long leftover = insert(menu, result, chunk, inventoryStart, inventoryEnd);
            if (leftover > 0) {
                ItemStack drop = result.copyWithCount((int) Math.min(leftover, result.getMaxStackSize()));
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }
            remainingOutput -= chunk;
        }

        int producedInt = (int) Math.min(produced, Integer.MAX_VALUE);
        result.onCraftedBy(player.level(), player, producedInt);
        player.awardStat(Stats.ITEM_CRAFTED.get(result.getItem()), crafts);
        if (resultSlots instanceof RecipeCraftingHolder holder) {
            holder.awardUsedRecipes(player, craftSlots.getItems());
        }
        EventHooks.firePlayerCraftingEvent(player, result, craftSlots);

        menu.slotsChanged(craftSlots);
        return true;
    }

    private static long countFreeSpace(AbstractContainerMenu menu, ItemStack result, int start, int end) {
        long space = 0L;
        for (int i = start; i < end; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            int limit = slot.getMaxStackSize(result);
            if (stack.isEmpty()) {
                space += limit;
            } else if (ItemStack.isSameItemSameComponents(stack, result)) {
                space += Math.max(0, limit - stack.getCount());
            }
        }
        return space;
    }

    private static long insert(AbstractContainerMenu menu, ItemStack result, int count, int start, int end) {
        int remaining = count;
        for (int i = start; i < end && remaining > 0; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            int limit = slot.getMaxStackSize(result);
            if (stack.isEmpty()) {
                int amount = Math.min(remaining, limit);
                slot.set(result.copyWithCount(amount));
                slot.setChanged();
                remaining -= amount;
            } else if (ItemStack.isSameItemSameComponents(stack, result)) {
                int amount = Math.min(remaining, limit - stack.getCount());
                if (amount > 0) {
                    stack.grow(amount);
                    slot.setChanged();
                    remaining -= amount;
                }
            }
        }
        return remaining;
    }
}
