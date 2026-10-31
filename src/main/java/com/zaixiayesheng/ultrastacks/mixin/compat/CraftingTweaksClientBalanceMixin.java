package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.crafting.GridBalance;

import com.zaixiayesheng.ultrastacks.net.BalancePayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import java.util.ArrayList;
import java.util.List;
import net.blay09.mods.craftingtweaks.api.CraftingGrid;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 真正卡人的是这里：合成辅助在客户端先“预演”一遍，靠的是一次一次模拟鼠标点击。
 * 格子里几万个的时候就是几万次点击。我们直接算好摆进格子，再给服务端发一句请求。
 *
 * <p>This is where the stall lives. CraftingTweaks previews the split on the client by replaying
 * it as mouse clicks, one item at a time. We write the finished layout straight into the grid and
 * tell the server once instead.</p>
 */
@Mixin(targets = "net.blay09.mods.craftingtweaks.client.ClientProvider", remap = false)
public abstract class CraftingTweaksClientBalanceMixin {

    private static final int VANILLA_STACK_SIZE = 64;

    @Inject(method = "balanceGrid(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/blay09/mods/craftingtweaks/api/CraftingGrid;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$balanceOnClient(Player player, AbstractContainerMenu menu, CraftingGrid grid, CallbackInfo ci) {
        ultrastacks$handle("balance", player, menu, grid, ci);
    }

    @Inject(method = "spreadGrid(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/blay09/mods/craftingtweaks/api/CraftingGrid;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$spreadOnClient(Player player, AbstractContainerMenu menu, CraftingGrid grid, CallbackInfo ci) {
        ultrastacks$handle("spread", player, menu, grid, ci);
    }

    private void ultrastacks$handle(String kind, Player player, AbstractContainerMenu menu, CraftingGrid grid, CallbackInfo ci) {
        try {
            int start = grid.getGridStartSlot(player, menu);
            int size = grid.getGridSize(player, menu);
            if (size <= 0 || start < 0) {
                return;
            }
            List<Integer> gridSlots = new ArrayList<>(size);
            boolean oversized = false;
            for (int i = 0; i < size; i++) {
                int menuSlot = start + i;
                if (menuSlot >= menu.slots.size()) {
                    break;
                }
                Slot slot = menu.getSlot(menuSlot);
                ItemStack stack = slot.getItem();
                // 空格子也要带上：平铺就是往这些空格里铺。
                if (!stack.isEmpty() && stack.getCount() > VANILLA_STACK_SIZE) {
                    oversized = true;
                }
                gridSlots.add(slot.getContainerSlot());
            }
            // 小堆交给它自己点，那点次数无所谓。
            if (!oversized) {
                return;
            }
            Container matrix = grid.getCraftingMatrix(player, menu);
            if (matrix == null) {
                return;
            }
            DiagLog.line("[diag] ct client " + kind + " enter: grid=" + size + " slots");
            long t0 = System.nanoTime();
            GridBalance.Result result = GridBalance.balance(matrix, gridSlots, "spread".equals(kind));
            menu.broadcastChanges();
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            DiagLog.line(String.format("[diag] ct client %s: %d slots, %d items, %.2f ms",
                    kind, result.slots(), result.items(), ms));
            ClientPacketDistributor.sendToServer(new BalancePayload());
            ci.cancel();
        } catch (Throwable failure) {
            DiagLog.lineThrottled("ct-client-" + kind + "-failed",
                    "[diag] ct client " + kind + " failed: " + failure);
        }
    }
}
