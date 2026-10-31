package com.zaixiayesheng.ultrastacks.crafting;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
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
 * 一次 Shift 点击把整批合完。
 * 原版一次只合一个，65536 个材料就得在一 tick 里跑 65536 轮，谁也扛不住。
     *
 * Finishes the whole batch in one shift-click. Vanilla does one craft per pass, which would mean
 * 65536 passes inside a single tick for a 65536 stack.
 */
public final class BulkCrafting {

    private BulkCrafting() {
    }

    /** 两个开关都开才接管。 */
    public static boolean enabled() {
        return UltraStacksConfig.craftingModule() && UltraStacksConfig.bulkCrafting();
    }

    /**
     * 把产物槽后面这批一次合完：材料够几次、背包放得下几次，取小的。
     *
     * Crafts as much as both the materials and the inventory allow, whichever is smaller.
     */
    public static boolean tryCraftAll(AbstractContainerMenu menu, CraftingContainer craftSlots,
                                      ResultContainer resultSlots, Player player, int inventoryStart, int inventoryEnd) {
        if (!enabled()) {
            return false;
        }

        // 客户端那份菜单只是同步过来的副本，真扣料必须只发生在服务端，不然两边各算一次。
        if (player.level().isClientSide()) {
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

        // 催化剂格（剪刀、工具、模具）：合一次还回来一件，不应限制次数，也不能扣数量。
        // Self-returning ingredients: they come back after each craft, so they neither limit the
        // batch nor get their count touched.
        boolean[] selfReturning = new boolean[craftSlots.getContainerSize()];
        int limitedByInput = Integer.MAX_VALUE;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack returned = i < remainingItems.size() ? remainingItems.get(i) : ItemStack.EMPTY;
            if (!returned.isEmpty() && ItemStack.isSameItemSameComponents(returned, stack)) {
                selfReturning[i] = true;
                continue;
            }
            limitedByInput = Math.min(limitedByInput, stack.getCount());
        }
        if (limitedByInput == Integer.MAX_VALUE) {
            return false;
        }

        int perCraft = Math.max(1, result.getCount());
        long freeSpace = countFreeSpace(menu, result, inventoryStart, inventoryEnd);
        int crafts = CraftingMath.capBySpace(limitedByInput, freeSpace, perCraft);
        // 没按住解锁键就把产物夹到配置值；按“产物个数”算，不是按次数（铁块拆锭一次出 9 个）。
        // Cap by produced items, not craft count — one craft can yield nine (iron block to ingots).
        boolean unlocked = UnlockState.isUnlocked(player);
        int cap = UltraStacksConfig.bulkCraftLimit();
        long planned = (long) crafts * perCraft;
        if (UltraStacksConfig.bulkCraftUnlockKey() && !unlocked && planned > cap) {
            crafts = Math.max(1, cap / perCraft);
        }
        DiagLog.line("[diag] bulk craft: " + result.getDescriptionId() + " input=" + limitedByInput
                + " freeSpace=" + freeSpace + " perCraft=" + perCraft + " crafts=" + crafts
                + " items=" + ((long) crafts * perCraft) + " cap=" + cap
                + " unlocked=" + unlocked + " grid=" + gridCounts(craftSlots));
        if (crafts <= 0) {
            return false;
        }

        long t0 = System.nanoTime();
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            if (!selfReturning[i] && !craftSlots.getItem(i).isEmpty()) {
                craftSlots.removeItem(i, crafts);
            }
        }
        long t1 = System.nanoTime();

        for (int i = 0; i < remainingItems.size() && i < craftSlots.getContainerSize(); i++) {
            ItemStack rem = remainingItems.get(i);
            if (rem.isEmpty() || selfReturning[i]) {
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

        long t2 = System.nanoTime();
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

        long t3 = System.nanoTime();
        int producedInt = (int) Math.min(produced, Integer.MAX_VALUE);
        result.onCraftedBy(player.level(), player, producedInt);
        player.awardStat(Stats.ITEM_CRAFTED.get(result.getItem()), crafts);
        if (resultSlots instanceof RecipeCraftingHolder holder) {
            holder.awardUsedRecipes(player, craftSlots.getItems());
        }
        EventHooks.firePlayerCraftingEvent(player, result, craftSlots);

        long t4 = System.nanoTime();
        // 先清空产物槽再让菜单重算，不然客户端会挂着上一次的产物图标（看着能拿，其实服务端是空的）。
        // Clear the result slot first, or the client keeps showing the old result icon.
        resultSlots.setItem(0, ItemStack.EMPTY);
        menu.slotsChanged(craftSlots);
        long t5 = System.nanoTime();
        DiagLog.line("[diag] bulk craft done: crafts=" + crafts + " ms consume=" + ms(t0, t1)
                + " remain=" + ms(t1, t2) + " insert=" + ms(t2, t3) + " book=" + ms(t3, t4)
                + " sync=" + ms(t4, t5));
        return true;
    }

    /** 合成格里有超过 64 的堆就 true。 */
    public static boolean hasOversizedStacks(Container grid) {
        for (int i = 0; i < grid.getContainerSize(); i++) {
            if (grid.getItem(i).getCount() > 64) {
                return true;
            }
        }
        return false;
    }

    private static String ms(long from, long to) {
        return String.format(java.util.Locale.ROOT, "%.1f", (to - from) / 1_000_000.0);
    }

    private static String gridCounts(CraftingContainer craftSlots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (!stack.isEmpty()) {
                sb.append(i).append('=').append(stack.getCount()).append(' ');
            }
        }
        return sb.toString().trim();
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
