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
 * Crafting module: finishes a whole batch with a single shift-click (Minecraft 1.20.1 / Forge).
 *
 * <p>Vanilla repeatedly calls {@code quickMoveStack}: every round re-resolves the recipe, consumes
 * materials and inserts output. With a 32k stack in the grid that is 32k rounds inside one tick,
 * which freezes the client and can time out the server. This module instead computes how many
 * crafts the inputs and the free space allow, consumes once, and inserts the produced items once.
 *
 * <p><b>中文</b> 合成模块（1.20.1 / Forge）：一次 Shift 点击把能合的整批合完。先算「材料够合几次」与
 * 「背包还能放几次」的较小值，然后一次扣材料、一次按堆叠上限分块塞产物。
 */
public final class BulkCrafting {

    private BulkCrafting() {
    }

    /** True when the crafting module is enabled in the config. */
    public static boolean enabled() {
        return UltraStacksConfig.craftingModule() && UltraStacksConfig.bulkCrafting();
    }

    /**
     * Tries to finish the whole batch behind the result slot.
     *
     * @return true when the batch was handled (caller returns an empty stack and the vanilla loop
     *     stops), false when vanilla should handle this click
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

    /** Free room for {@code result} in the player inventory, accumulated in long. */
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

    /** Inserts up to {@code count} items into the inventory range; returns the leftovers. */
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
