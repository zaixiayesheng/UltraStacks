package com.zaixiayesheng.ultrastacks.crafting;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * 合成模块：一次 Shift 点击把整批材料合完。
 *
 * <p>原版每合一次就走一轮 {@code quickMoveStack}：重新匹配配方、扣一次料、塞一次产物。材料堆到
 * 32k 时等于一 tick 跑 32k 轮。这里先算「材料够合几次」与「背包放得下几次」的较小值，一次扣料，
 * 再把产物按堆叠上限分块塞回背包，塞不下的部分直接丢给玩家。
 *
 * <p>Finishes the whole batch in one shift-click. Vanilla runs one {@code quickMoveStack} round per
 * craft, re-matching the recipe and then consuming and inserting once each time, which for a 32k
 * input means 32k rounds in a single tick. This takes the smaller of "how many crafts the input
 * affords" and "how many the inventory can hold", consumes the materials once, then inserts the
 * output in chunks of at most one stack and hands whatever does not fit to the player as a drop.
 */
public final class BulkCrafting {

    private BulkCrafting() {
    }

    public static boolean enabled() {
        return UltraStacksConfig.craftingModule() && UltraStacksConfig.bulkCrafting();
    }

    /**
     * 尝试把产物槽背后那一批一次性合完。
     *
     * <p>调用方是 {@code quickMoveStack} 的 HEAD 注入点（工作台与背包各一个 mixin）。返回 true 时调用方
     * 应把结果设成空堆叠，原版循环随之结束。
     *
     * <p>Tries to finish the whole batch behind the result slot. The callers are the {@code HEAD}
     * injections into {@code quickMoveStack} (one each for the crafting table and the inventory grid);
     * on {@code true} they must return an empty stack so the vanilla loop stops.
     *
     * @param menu 本次合成所在的菜单。The menu being crafted in.
     * @param craftSlots 合成格。The crafting grid.
     * @param resultSlots 产物槽。The result slot container.
     * @param player 发起这次合成的玩家。The player who started the craft.
     * @param inventoryStart 背包区间起点（含）。First inventory slot, inclusive.
     * @param inventoryEnd 背包区间终点（不含）。Last inventory slot, exclusive.
     * @return 整批已处理为 true；应交回原版处理为 false。True when the batch was handled, false when vanilla should handle this click.
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

        NonNullList<ItemStack> remainingItems = NonNullList.withSize(9, ItemStack.EMPTY);
        Optional<CraftingRecipe> found =
                player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftSlots, player.level());
        if (found.isPresent()) {
            NonNullList<ItemStack> computed = found.get().getRemainingItems(craftSlots);
            for (int i = 0; i < Math.min(computed.size(), remainingItems.size()); i++) {
                remainingItems.set(i, computed.get(i));
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
            long totalGave = (long) rem.getCount() * crafts;
            while (totalGave > 0) {
                int chunk = CraftingMath.nextChunk(totalGave, rem.getMaxStackSize());
                ItemStack gave = rem.copyWithCount(chunk);
                ItemStack current = craftSlots.getItem(i);
                if (current.isEmpty()) {
                    craftSlots.setItem(i, gave);
                } else if (ItemStack.isSameItemSameTags(current, gave)) {
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
                totalGave -= chunk;
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
        ForgeEventFactory.firePlayerCraftingEvent(player, result, craftSlots);
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
            } else if (ItemStack.isSameItemSameTags(stack, result)) {
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
            } else if (ItemStack.isSameItemSameTags(stack, result)) {
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
