package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.crafting.GridBalance;
import net.blay09.mods.craftingtweaks.api.CraftingGrid;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 精妙存储/背包的合成升级有自己的均分实现，跟我们改的那份是两套代码。
 *
 * <p>它的算法和合成辅助原版一模一样：先算平均值，再把余数一格一格加回去，每加一次都要看
 * 这一堆自己的上限。上限被抬到 65536 以后这段就一直在空转，所以在这个界面点均分照样卡。
 * 这里同样只在出现超过 64 的堆时接手，一次算准。</p>
 *
 * <p>Sophisticated storage's crafting upgrade ships its own balance handler, a copy of the
 * vanilla CraftingTweaks one. Same per-slot probing loop, same slowdown once stacks go past 64.
 * We take over only when a stack is actually oversized.</p>
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks.CraftingUpgradeTweakProvider$StorageCraftingGridBalanceHandler", remap = false)
public abstract class SophisticatedCraftingBalanceMixin {

    private static final int VANILLA_STACK_SIZE = 64;

    @Inject(method = "balanceGrid(Lnet/blay09/mods/craftingtweaks/api/CraftingGrid;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$balanceSophisticatedGrid(CraftingGrid grid, Player player, AbstractContainerMenu menu, CallbackInfo ci) {
        try {
            // 先记一笔，按钮点到就一定看得到。
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
            if (result == null) {
                return;
            }
            menu.broadcastChanges();
            DiagLog.line("[diag] soph balance: " + result.slots() + " slots, " + result.items()
                    + " items, " + String.format(java.util.Locale.ROOT, "%.2f", (System.nanoTime() - start) / 1_000_000.0) + " ms");
            ci.cancel();
        } catch (Throwable t) {
            DiagLog.lineThrottled("soph-balance-failed", "[diag] soph balance failed: " + t);
        }
    }
}
