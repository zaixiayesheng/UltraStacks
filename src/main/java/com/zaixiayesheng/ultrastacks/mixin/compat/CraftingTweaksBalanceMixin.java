package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.crafting.GridBalance;

import com.zaixiayesheng.ultrastacks.net.BalancePayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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
 * 合成辅助自己的均分：balance 和 spread 两个按钮都走这个类，大堆时我们接手。
 * 它补余数是一格一个地长，还要问“这堆自己的上限”，格子里几万个就能磨半天。
 *
 * <p>Takes over both of CraftingTweaks' split buttons once a stack is bigger than 64.</p>
 */
@Mixin(targets = "net.blay09.mods.craftingtweaks.api.impl.DefaultGridBalanceHandler", remap = false)
public abstract class CraftingTweaksBalanceMixin {

    private static final int VANILLA_STACK_SIZE = 64;

    @Inject(method = "balanceGrid(Lnet/blay09/mods/craftingtweaks/api/CraftingGrid;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$balanceBigStacks(CraftingGrid grid, Player player, AbstractContainerMenu menu, CallbackInfo ci) {
        ultrastacks$handle("balance", grid, player, menu, ci);
    }

    @Inject(method = "spreadGrid(Lnet/blay09/mods/craftingtweaks/api/CraftingGrid;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$spreadBigStacks(CraftingGrid grid, Player player, AbstractContainerMenu menu, CallbackInfo ci) {
        ultrastacks$handle("spread", grid, player, menu, ci);
    }

    private void ultrastacks$handle(String kind, CraftingGrid grid, Player player, AbstractContainerMenu menu, CallbackInfo ci) {
        try {
            DiagLog.line("[diag] ct " + kind + " enter: menu=" + menu.getClass().getSimpleName()
                    + " side=" + (player.level().isClientSide() ? "client" : "server"));
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
                // 空格留着，平铺要用。
                if (!stack.isEmpty() && stack.getMaxStackSize() > 1 && stack.getCount() > VANILLA_STACK_SIZE) {
                    bigStackSeen = true;
                }
                gridSlots.add(containerSlot);
            }
            if (!bigStackSeen) {
                return;
            }
            long t0 = System.nanoTime();
            GridBalance.Result result = GridBalance.balance(matrix, gridSlots, "spread".equals(kind));
            menu.broadcastChanges();
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            String line = String.format("ct %s: %d slots, %d items, %.2f ms", kind, result.slots(), result.items(), ms);
            UltraStacks.LOGGER.info("[diag] {}", line);
            DiagLog.line("[diag] " + line);
            if (player.level().isClientSide()) {
                ClientPacketDistributor.sendToServer(new BalancePayload());
            }
            ci.cancel();
        } catch (Throwable failure) {
            UltraStacks.LOGGER.warn("[diag] ct {} failed, letting CraftingTweaks handle it", kind, failure);
        }
    }
}
