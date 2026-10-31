package com.zaixiayesheng.ultrastacks.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 均分合成格：把同物品的几堆摊平，余数一轮分完。
 *
 * <p>先按物品分组求和，算出每格该放多少，再把余数均匀补上；不去逐个问“这堆还能不能再加一个”，
 * 所以格子里有几万个也不会磨。
 *
 * <p>Spreads the crafting grid flat in one pass: group by item, divide, hand out the remainder.
 * No per-item probing, so tens of thousands of items cost nothing.
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

    /** 把给定的格子摊平。 */
    public static Result balance(Container matrix, List<Integer> gridSlots) {
        boolean[] used = new boolean[gridSlots.size()];
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

            int cap = Math.max(1, template.getItem().getDefaultMaxStackSize());
            int per = (int) Math.min(cap, sum / group.size());
            long remainder = sum - (long) per * group.size();
            for (int slot : group) {
                matrix.setItem(slot, template.copyWithCount(per));
            }
            int index = 0;
            int guard = group.size();
            while (remainder > 0 && guard > 0) {
                int slot = group.get(index);
                ItemStack current = matrix.getItem(slot);
                if (!current.isEmpty() && current.getCount() < cap) {
                    matrix.setItem(slot, current.copyWithCount(current.getCount() + 1));
                    remainder--;
                    guard = group.size();
                } else {
                    guard--;
                }
                index = (index + 1) % group.size();
            }
            total += per * group.size() + (int) (sum - (long) per * group.size() - remainder);
        }
        return new Result(gridSlots.size(), total);
    }
}
