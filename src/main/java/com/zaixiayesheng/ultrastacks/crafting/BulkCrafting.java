package com.zaixiayesheng.ultrastacks.crafting;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * 合成模块：一次 Shift 点击把整批材料一次合完。
 *
 * <p>原版用 {@code while} 循环反复调用 {@code quickMoveStack}，每一轮重新匹配配方、扣一套料、塞一次
 * 产物。材料堆到 65536 时就是一个 tick 里跑 65536 轮配方匹配加扣料，tick 时间被拖到肉眼可见。这里先算
 * 出「材料够合几次」与「背包还放得下几次」的较小值，一次性扣料，再把产物按单堆上限拆块插入，塞不进去
 * 的部分直接掉给玩家，最后补上合成统计与容器变更通知。
 *
 * <p>The crafting module: finishes an entire batch of crafts from a single shift-click.
 *
 * <p>Vanilla keeps calling {@code quickMoveStack} from a {@code while} loop, and every round matches the
 * recipe again, consumes one set of ingredients and pushes out one result. With ingredients stacked to
 * 65536 that is 65536 rounds of recipe matching and consumption inside a single tick, which stretches
 * tick time to something you can see. This implementation first works out the smaller of "how many
 * crafts the ingredients allow" and "how many crafts the inventory can still hold", consumes the
 * ingredients once, splits the output into per-stack-sized chunks when inserting it, hands whatever does
 * not fit straight to the player, and finally reports the crafted stats and notifies the container of the
 * change.
 */
public final class BulkCrafting {

    private BulkCrafting() {
    }

    public static boolean enabled() {
        return UltraStacksConfig.craftingModule() && UltraStacksConfig.bulkCrafting();
    }

    /**
     * 尝试把结果槽后面的整批材料合成完。
     *
     * <p>只在服务端执行；配方匹配失败、格子为空或算出的次数为 0 时返回 false，让原版逻辑接手这一下点击。
     * 合成剩余物（例如桶、空瓶）会先按次数补齐后放回原格，放不下的进背包或掉在地上。
     *
     * <p>Tries to finish the whole batch behind the result slot.
     *
     * <p>This only runs on the server side. It returns false when no recipe matches, when the grid is
     * empty, or when the computed craft count is zero, so that vanilla handles this click. Crafting
     * remainders such as buckets and empty bottles are multiplied by the craft count, put back into their
     * grid slots first, and go to the inventory or the ground when they do not fit.
     *
     * @return true 表示这批已处理完（调用方应返回空堆叠，原版循环随之结束），false 表示交给原版处理 /
     *     true when the batch was handled (the caller returns an empty stack and the vanilla loop
     *     stops), false when vanilla should handle this click
     */
    public static boolean tryCraftAll(AbstractCraftingMenu menu, Player player, int inventoryStart, int inventoryEnd) {
        if (!enabled()) {
            return false;
        }

        Slot resultSlot = menu.getResultSlot();
        ItemStack result = resultSlot.getItem();
        if (result.isEmpty() || result.getCount() <= 0) {
            return false;
        }

        if (menu.getInputGridSlots().isEmpty()) {
            return false;
        }
        Container gridContainer = menu.getInputGridSlots().get(0).container;
        if (!(gridContainer instanceof CraftingContainer craftSlots)) {
            return false;
        }
        CraftingInput input = craftSlots.asCraftInput();

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        RecipeManager recipeManager = serverLevel.recipeAccess();

        NonNullList<ItemStack> remainingItems = NonNullList.withSize(9, ItemStack.EMPTY);
        Optional<RecipeHolder<CraftingRecipe>> found =
                recipeManager.getRecipeFor(RecipeType.CRAFTING, input, serverLevel);
        found.ifPresent(holder -> {
            NonNullList<ItemStack> computed = holder.value().getRemainingItems(input);
            for (int i = 0; i < Math.min(computed.size(), remainingItems.size()); i++) {
                remainingItems.set(i, computed.get(i));
            }
        });

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
        result.onCraftedBy(player, producedInt);
        player.awardStat(Stats.ITEM_CRAFTED.get(result.getItem()), crafts);
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
