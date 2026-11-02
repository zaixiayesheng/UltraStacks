package com.zaixiayesheng.ultrastacks.mixin.probe;

import com.zaixiayesheng.ultrastacks.DiagLog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 只读探针：统计精妙容器一次 Shift 里 quickMoveStack 被调了几次、每次的槽位与产物数量。
 *
 * <p>Read-only probe: counts how often quickMoveStack runs inside one Shift click on the
 * Sophisticated container, with the slot index and result size of each call.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class SophQuickMoveProbeMixin {
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static volatile long WINDOW_START = System.currentTimeMillis();

    @Inject(method = "quickMoveStack", at = @At("HEAD"), require = 0)
    private void ultrastacks$probeQuickMove(Player player, int index, CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        int n = CALLS.incrementAndGet();
        long now = System.currentTimeMillis();
        if (now - WINDOW_START > 1000L) {
            DiagLog.line("[diag] soph quickmove: " + n + " calls in " + (now - WINDOW_START) + " ms");
            CALLS.set(0);
            WINDOW_START = now;
        }
        if (n <= 3) {
            String detail = "n/a";
            try {
                Slot slot = ((net.minecraft.world.inventory.AbstractContainerMenu) (Object) this).getSlot(index);
                detail = "slotClass=" + slot.getClass().getSimpleName() + " stack=" + slot.getItem().getCount()
                        + " max=" + slot.getItem().getMaxStackSize();
            } catch (Throwable ignored) {
                // 忽略 / ignore
            }
            DiagLog.line("[diag] soph quickmove call #" + n + " index=" + index + " " + detail);
        }
    }
}
