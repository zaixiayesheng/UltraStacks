package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.crafting.GridBalance;
import com.zaixiayesheng.ultrastacks.net.BalancePayload;
import java.util.ArrayList;
import java.util.List;
import net.blay09.mods.craftingtweaks.api.CraftingGrid;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 合成辅助的均分，大堆的时候我们接手。
 * 它补余数是一格一个地长，还要问“这堆自己的上限”，格子里几万个就能磨半天。
 * 超过 64 的堆我们自己算一遍，一次分完；小堆照旧交给它。
 * 算完再给服务端递一句话——那边没装合成辅助的话，它自己的包会被丢掉，得我们来补。
 *
 * <p>Takes over CraftingTweaks' balance pass once a stack is bigger than 64. Its remainder loop
 * grows one item at a time against each stack's own limit, which crawls on big grids. Afterwards we
 * ping the server too: without CraftingTweaks there, its own packet never lands.
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
            GridBalance.Result result = GridBalance.balance(matrix, gridSlots);
            menu.broadcastChanges();
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            String line = String.format("ct balance: %d slots, %d items, %.2f ms", result.slots(), result.items(), ms);
            UltraStacks.LOGGER.info("[diag] {}", line);
            DiagLog.line("[diag] " + line);
            // 客户端摊完还要告诉服务端一声，否则没装合成辅助的服务器会把结果冲回去。
            if (player.level().isClientSide()) {
                ClientPacketDistributor.sendToServer(new BalancePayload());
            }
            ci.cancel();
        } catch (Throwable failure) {
            // 出问题就把这次操作还给合成辅助自己，别把玩家的合成格卡住。
            UltraStacks.LOGGER.warn("[diag] ct balance failed, letting CraftingTweaks handle it", failure);
        }
    }
}
