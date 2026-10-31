package com.zaixiayesheng.ultrastacks.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 分摊合成格：同物品的几堆一次算完，余数一轮分掉。
 *
 * <p>先按物品分组求和，算出每格该放多少，再用取整的余数给前几格各加一个——不去逐格问
 * 「这堆还能不能再加一个」，所以格子里有几万个也只是一次除法。
 * 只有一堆、旁边还空着的时候，会把空格也算进目标，这就是「平铺」。
 *
 * <p>Spreads the crafting grid in one pass: group by item, divide, then hand out the remainder.
 * No per-item probing, so tens of thousands of items cost one division. A lone stack next to
 * empty slots also fills those empties — that's the spread button.
 */
public final class GridBalance {

    /** 结果：动了几格、摊了多少东西。 */
    public record Result(int slots, int items) {
    }

    private GridBalance() {
    }

    /** 从菜单里找出合成格并摊平；找不到合成格返回 null。 */
    public static Result balanceMenu(AbstractContainerMenu menu) {
        Container matrix = null;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.getSlot(i);
            if (!(slot.container instanceof CraftingContainer craftMatrix)) {
                continue;
            }
            // 结果槽用的是 ResultContainer，不会走到这里。
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
        return balance(matrix, slots);
    }

    /** 均分：只动本来就有这类物品的格子。 */
    public static Result balance(Container matrix, List<Integer> gridSlots) {
        return balance(matrix, gridSlots, false);
    }

    /**
     * 把给定的格子摊平。
     *
     * @param spread 平铺时才把空格也算成目标；均分只动原来就有同类物品的格子
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
                if (other.isEmpty() || !ItemStack.isSameItemSameComponents(template, other)) {
                    continue;
                }
                used[k] = true;
                group.add(gridSlots.get(k));
                sum += other.getCount();
            }
            if (group.isEmpty()) {
                continue;
            }

            // 平铺才吃空格；均分保持原来那几格。
            List<Integer> targets = new ArrayList<>(group);
            if (spread && !empties.isEmpty()) {
                for (int index : new ArrayList<>(empties)) {
                    used[index] = true;
                    targets.add(gridSlots.get(index));
                    empties.remove(Integer.valueOf(index));
                }
            }

            // 每格能放多少，取这几格里最大的实际上限——不能比现有的堆还小，否则一摊就缩水。
            int cap = 1;
            for (int index : targets) {
                cap = Math.max(cap, matrix.getItem(index).getMaxStackSize());
            }
            long n = targets.size();
            long per = Math.max(0L, Math.min((long) cap, sum / n));
            long[] counts = new long[targets.size()];
            java.util.Arrays.fill(counts, per);
            // 余数先一格发一个（这样 5 个东西摊 9 格就是前 5 格各 1 个，而不是全塞第一格）。
            long leftover = sum - per * n;
            for (int i = 0; i < n && leftover > 0; i++) {
                if (counts[i] < cap) {
                    counts[i]++;
                    leftover--;
                }
            }
            // 还有剩说明碰到了上限，谁有空位就给谁。
            for (int i = 0; i < n && leftover > 0; i++) {
                long room = Math.max(0L, (long) cap - counts[i]);
                long add = Math.min(room, leftover);
                counts[i] += add;
                leftover -= add;
            }
            // 实在放不下就压回第一格：宁可一格冒顶，也不能让玩家丢东西。
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
}
