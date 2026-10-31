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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 精妙存储/背包的合成升级自带一份均分，算法跟合成辅助原版一模一样，大堆时照样磨。
 * 这里同样只在出现超过 64 的堆时接手，一次算准。
 *
 * <p>Sophisticated's crafting upgrade ships its own balance handler, a copy of the vanilla one.
 * Same per-slot probing loop, same slowdown once stacks go past 64.</p>
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore/compat/craftingtweaks/CraftingUpgradeTweakProvider$StorageCraftingGridBalanceHandler", remap = false)
public abstract class SophisticatedCraftingBalanceMixin {

    private static final int VANILLA_STACK_SIZE = 64;

    @Inject(method = "balanceGrid(Lnet/blay09/mods/craftingtweaks/api/CraftingGrid;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$balanceSophisticatedGrid(CraftingGrid grid, Player player, AbstractContainerMenu menu, CallbackInfo ci) {
        try {
            DiagLog.line("[diag] soph balance enter: menu=" + menu.getClass().getSimpleName()
                    + " side=" + (player.level().isClientSide() ? "client" : "server"));
            Container matrix = grid.getCraftingMatrix(player, menu);
            if (matrix == null) {
                return;
            }
            int size = grid.getGridSize(player, menu);
            if (size <= 0) {
                return;
            }
            List<Integer> slots = new ArrayList<>();
            int limit = Math.min(size, matrix.getContainerSize());
            boolean oversized = false;
            for (int i = 0; i < limit; i++) {
                slots.add(i);
                if (matrix.getItem(i).getCount() > VANILLA_STACK_SIZE) {
                    oversized = true;
                }
            }
            if (!oversized) {
                return;
            }
            long start = System.nanoTime();
            GridBalance.Result result = GridBalance.balance(matrix, slots);
            menu.broadcastChanges();
            DiagLog.line("[diag] soph balance: " + result.slots() + " slots, " + result.items()
                    + " items, " + String.format(java.util.Locale.ROOT, "%.2f", (System.nanoTime() - start) / 1_000_000.0) + " ms");
            ClientPacketDistributor.sendToServer(new BalancePayload());
            ci.cancel();
        } catch (Throwable t) {
            DiagLog.lineThrottled("soph-balance-failed", "[diag] soph balance failed: " + t);
        }
    }
}
