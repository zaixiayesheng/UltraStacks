package com.zaixiayesheng.ultrastacks.crafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 摊合成格：同物品的几堆一次算完，余数一格发一个。
 *
 * <p>均分只动原本就有这类物品的格子，平铺才把空格也算进来。先按组求和、一次除法定每格多少，
 * 剩下的按“谁还有空位”发下去，实在放不下就压在第一格——绝不把物品凭空删掉。</p>
 *
 * <p>One pass over the crafting grid. Balance touches only the slots that already hold the item,
 * spread also fills the empty ones. The remainder is handed out slot by slot, and anything that
 * still does not fit goes back into the first slot instead of being dropped.</p>
 */
public final class GridBalance {

    /** 结果：动了几格、摊了多少东西。 */
    public record Result(int slots, int items) {
    }

    private GridBalance() {
    }

    /** 从菜单里找出合成格并均分；找不到合成格返回 null。 */
    public static Result balanceMenu(AbstractContainerMenu menu) {
        Container matrix = null;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.getSlot(i);
            if (!(slot.container instanceof CraftingContainer craftMatrix)) {
                continue;
            }
            if (matrix == null) {
                matrix = craftMatrix;
            }
            if (matrix == craftMatrix) {
                slots.add(slot.getContainerSlot());
            }
        }
        if (matrix == null || slots.isEmpty()) {
            return null;
        }
        return balance(matrix, slots, false);
    }

    /** 均分：只动本来就有这类物品的格子。 */
    public static Result balance(Container matrix, List<Integer> gridSlots) {
        return balance(matrix, gridSlots, false);
    }

    /**
     * 摊平给定格子。
     *
     * @param spread 平铺时把空格也算成目标；均分只动原来就有同类物品的格子
     */
    public static Result balance(Container matrix, List<Integer> gridSlots, boolean spread) {
        boolean[] used = new boolean[gridSlots.size()];
        List<Integer> empties = new ArrayList<>();
        for (int i = 0; i < gridSlots.size(); i++) {
            if (matrix.getItem(gridSlots.get(i)).isEmpty()) {
                empties.add(i);
            }
        }

        int total = 0;
        for (int g = 0; g < gridSlots.size(); g++) {
            if (used[g]) {
                continue;
            }
            ItemStack template = matrix.getItem(gridSlots.get(g));
            if (template.isEmpty() || template.getMaxStackSize() <= 1) {
                continue;
            }

            long sum = 0;
            List<Integer> group = new ArrayList<>();
            for (int k = g; k < gridSlots.size(); k++) {
                if (used[k]) {
                    continue;
                }
                ItemStack other = matrix.getItem(gridSlots.get(k));
                if (other.isEmpty() || !sameItem(template, other)) {
                    continue;
                }
                used[k] = true;
                group.add(gridSlots.get(k));
                sum += other.getCount();
            }
            if (group.isEmpty()) {
                continue;
            }

            List<Integer> targets = new ArrayList<>(group);
            if (spread && !empties.isEmpty()) {
                for (int index : new ArrayList<>(empties)) {
                    used[index] = true;
                    targets.add(gridSlots.get(index));
                    empties.remove(Integer.valueOf(index));
                }
            }

            int cap = capOf(matrix, targets, template);
            long n = targets.size();
            long per = Math.max(0L, Math.min((long) cap, sum / n));
            long[] counts = new long[targets.size()];
            Arrays.fill(counts, per);
            // 余数先一格发一个：5 个摊 9 格就是前 5 格各 1，而不是全塞第一格。
            long leftover = sum - per * n;
            for (int i = 0; i < n && leftover > 0; i++) {
                if (counts[i] < cap) {
                    counts[i]++;
                    leftover--;
                }
            }
            // 撞上限了就让还有空位的格子接着吃。
            for (int i = 0; i < n && leftover > 0; i++) {
                long room = Math.max(0L, (long) cap - counts[i]);
                long add = Math.min(room, leftover);
                counts[i] += add;
                leftover -= add;
            }
            // 还是放不下就压回第一格，绝不删物品。
            if (leftover > 0) {
                counts[0] += leftover;
            }
            for (int i = 0; i < n; i++) {
                int count = (int) Math.min(Integer.MAX_VALUE, counts[i]);
                matrix.setItem(targets.get(i), count <= 0 ? ItemStack.EMPTY : template.copyWithCount(count));
                total += count;
            }
        }
        return new Result(gridSlots.size(), total);
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    /** 这几格里最大的实际容量，不能比现有的堆还小，否则一摊就缩水。 */
    private static int capOf(Container matrix, List<Integer> targets, ItemStack template) {
        int cap = Math.max(1, template.getMaxStackSize());
        for (int index : targets) {
            cap = Math.max(cap, matrix.getItem(index).getMaxStackSize());
        }
        return cap;
    }
}
