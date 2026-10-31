package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacks;
import java.util.ArrayList;
import java.util.List;
import net.blay09.mods.craftingtweaks.api.CraftingGrid;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 合成辅助的均分，大堆的时候我们接手。
 * 它补余数是一格一个地长，还要问“这堆自己的上限”，格子里几万个就能磨半天。
 * 超过 64 的堆我们自己算一遍，一次分完；小堆照旧交给它。
     *
 * Takes over CraftingTweaks' balance pass once a stack is bigger than 64.
 * Its remainder loop grows one item at a time against each stack's own limit, which crawls
 * when the grid holds tens of thousands. Small grids are left alone.
 */
@Mixin(targets = "net.blay09.mods.craftingtweaks.api.impl.DefaultGridBalanceHandler", remap = false)
public abstract class CraftingTweaksBalanceMixin {

    /** 原版堆叠上限，超过才值得接手。 */
    private static final int VANILLA_STACK_SIZE = 64;

    @Inject(method = "balanceGrid(Lnet/blay09/mods/craftingtweaks/api/CraftingGrid;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$balanceBigStacks(CraftingGrid grid, Player player, AbstractContainerMenu menu, CallbackInfo ci) {
        try {
            Container matrix = grid.getCraftingMatrix(player, menu);
            if (matrix == null) {
                return;
            }
            int start = grid.getGridStartSlot(player, menu);
            int size = grid.getGridSize(player, menu);
            if (size <= 0 || start < 0) {
                return;
            }

            List<Integer> gridSlots = new ArrayList<>(size);
            boolean bigStackSeen = false;
            for (int i = 0; i < size; i++) {
                int menuSlot = start + i;
                if (menuSlot >= menu.slots.size()) {
                    break;
                }
                int containerSlot = menu.getSlot(menuSlot).getContainerSlot();
                ItemStack stack = matrix.getItem(containerSlot);
                if (stack.isEmpty() || stack.getMaxStackSize() <= 1) {
                    continue;
                }
                if (stack.getCount() > VANILLA_STACK_SIZE) {
                    bigStackSeen = true;
                }
                gridSlots.add(containerSlot);
            }
            if (!bigStackSeen) {
                return;
            }

            long t0 = System.nanoTime();
            int total = ultrastacks$spread(matrix, gridSlots);
            menu.broadcastChanges();
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            String line = String.format("ct balance: %d slots, %d items, %.2f ms", gridSlots.size(), total, ms);
            UltraStacks.LOGGER.info("[diag] {}", line);
            DiagLog.line("[diag] " + line);
            ci.cancel();
        } catch (Throwable failure) {
            // 出问题就把这次操作还给合成辅助自己，别把玩家的合成格卡住。
            UltraStacks.LOGGER.warn("[diag] ct balance failed, letting CraftingTweaks handle it", failure);
        }
    }

    /** 按物品分组摊平，余数一次分完。 */
    private static int ultrastacks$spread(Container matrix, List<Integer> gridSlots) {
        boolean[] used = new boolean[gridSlots.size()];
        int total = 0;
        for (int g = 0; g < gridSlots.size(); g++) {
            if (used[g]) {
                continue;
            }
            ItemStack template = matrix.getItem(gridSlots.get(g));
            if (template.isEmpty()) {
                continue;
            }
            long sum = 0;
            List<Integer> group = new ArrayList<>();
            for (int k = g; k < gridSlots.size(); k++) {
                if (used[k]) {
                    continue;
                }
                ItemStack other = matrix.getItem(gridSlots.get(k));
                if (other.isEmpty() || !ItemStack.isSameItemSameTags(template, other)) {
                    continue;
                }
                used[k] = true;
                group.add(gridSlots.get(k));
                sum += other.getCount();
            }
            if (group.isEmpty()) {
                continue;
            }

            int cap = Math.max(1, template.getItem().getMaxStackSize());
            int per = (int) Math.min(cap, sum / group.size());
            long remainder = sum - (long) per * group.size();
            for (int slot : group) {
                matrix.setItem(slot, template.copyWithCount(per));
            }
            // 余数一格一个地补，补满一圈就停，绝不空转。
            int index = 0;
            int guard = group.size();
            while (remainder > 0 && guard > 0) {
                int slot = group.get(index);
                ItemStack current = matrix.getItem(slot);
                if (current.getCount() < cap) {
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
        return total;
    }
}
