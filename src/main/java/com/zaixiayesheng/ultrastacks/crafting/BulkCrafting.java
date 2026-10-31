package com.zaixiayesheng.ultrastacks.crafting;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
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
 * 一次 Shift 点击把整批合完。
 * 原版一次只合一个，65536 个材料就得在一 tick 里跑 65536 轮，谁也扛不住。
     *
 * Finishes the whole batch in one shift-click. Vanilla does one craft per pass, which would mean
 * 65536 passes inside a single tick for a 65536 stack.
 */
public final class BulkCrafting {

    private BulkCrafting() {
    }

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

        NonNullList<ItemStack> remainingItems = NonNullList.withSize(9, ItemStack.EMPTY);
        Optional<CraftingRecipe> found =
                player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftSlots, player.level());
        if (found.isPresent()) {
            NonNullList<ItemStack> computed = found.get().getRemainingItems(craftSlots);
            for (int i = 0; i < Math.min(computed.size(), remainingItems.size()); i++) {
                remainingItems.set(i, computed.get(i));
            }
        }

        // 有些配方某一格是「催化剂」：合一次拿走一份，收尾又把同一件东西原样还回来（剪刀、工具、模具之类）。
        // 这种格子不消耗材料、也不该限制合成次数，按老算法它只有 1 个就永远只能合一次；
        // 同时它也不能被扣数量，否则还回来的时候会凭空多一堆。
        // Some recipes keep an ingredient: it is taken once per craft and handed straight back by
        // getRemainingItems (scissors, tools, moulds). Such a slot neither limits the batch nor loses
        // count — otherwise the recipe would only ever craft once and the returned stack would duplicate.
        boolean[] selfReturning = new boolean[craftSlots.getContainerSize()];
        int limitedByInput = Integer.MAX_VALUE;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack returned = i < remainingItems.size() ? remainingItems.get(i) : ItemStack.EMPTY;
            if (!returned.isEmpty() && ItemStack.isSameItemSameTags(returned, stack)) {
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
        // 保险：没按住解锁键就只合配置里的次数，免得手快把几十万材料一把清空。
        boolean unlocked = UnlockState.isUnlocked(player);
        int cap = UltraStacksConfig.bulkCraftLimit();
        long planned = (long) crafts * perCraft;
        if (UltraStacksConfig.bulkCraftUnlockKey() && !unlocked && planned > cap) {
            crafts = Math.max(1, cap / perCraft);
        }
        DiagLog.line("[diag] bulk craft: " + result.getDescriptionId() + " input=" + limitedByInput
                + " freeSpace=" + freeSpace + " perCraft=" + perCraft + " crafts=" + crafts
                + " items=" + ((long) crafts * perCraft) + " cap=" + cap + " unlocked=" + unlocked);
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
        ForgeEventFactory.firePlayerCraftingEvent(player, result, craftSlots);
        long t4 = System.nanoTime();
        // 先清产物槽再让菜单重算：格子空了就该是空的，没空的话 slotsChanged 会把产物填回来。
        // 少了这步客户端会留着上一次的图标——看着能拿，点下去服务端已经是空的。
        // Clear the result slot before the menu recomputes: an empty grid must read as empty, a grid
        // with leftovers gets its result filled back. Without this the client keeps a stale icon that
        // looks takeable while the server has nothing.
        resultSlots.setItem(0, ItemStack.EMPTY);
        menu.slotsChanged(craftSlots);
        long t5 = System.nanoTime();
        DiagLog.line("[diag] bulk craft done: crafts=" + crafts + " ms consume=" + ms(t0, t1)
                + " remain=" + ms(t1, t2) + " insert=" + ms(t2, t3) + " book=" + ms(t3, t4)
                + " sync=" + ms(t4, t5));
        return true;
    }

    private static String ms(long from, long to) {
        return String.format(java.util.Locale.ROOT, "%.1f", (to - from) / 1_000_000.0);
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
