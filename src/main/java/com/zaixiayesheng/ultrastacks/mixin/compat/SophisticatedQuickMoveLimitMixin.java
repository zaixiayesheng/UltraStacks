package com.zaixiayesheng.ultrastacks.mixin.compat;


import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.crafting.BatchCounter;
import com.zaixiayesheng.ultrastacks.crafting.UnlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙界面的 Shift 批量走它自己的 {@code quickMoveStack} 循环，上限在这里按「真正搬出东西的次数」记：
 * 进去前先看有没有到上限，回来时只有当次真的搬出了产物才记一份，这样 64 次就是实实在在的 64 份。
 *
 * <p>Sophisticated's Shift crafting loops on its own {@code quickMoveStack}. The cap is counted here by
 * successful moves: check before the call, and only count a run when the call actually moved something,
 * so a limit of 64 really means 64 runs.
 *
 * <p>1.20.1 的精妙 jar 里这个方法带的是 SRG 名（{@code m_7648_}），因为它是对着混淆后的原版编的。
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class SophisticatedQuickMoveLimitMixin {
    @Inject(method = "m_7648_(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$capRuns(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        // 精妙在进入自己的循环前会先合成一次，那一次不经过这里；所以这条路径多放一次，
        // 使「循环外的 1 次 + 循环里的 N 次」正好等于配置的上限。
        // Sophisticated crafts once before entering its own loop, which never reaches this hook, so this
        // path allows exactly one extra move: the outside craft plus the loop equals the configured cap.
        boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey() && UnlockState.isUnlocked(player);
        long limit = Math.max(1, UltraStacksConfig.bulkCraftLimit());
        long counted = BatchCounter.count(player.getUUID());
        String where = "index=" + index;
        net.minecraft.world.inventory.Slot sophSlot =
            ((net.minecraft.world.inventory.AbstractContainerMenu) (Object) this).getSlot(index);
        if (sophSlot != null) {
            where += " containerSlot=" + sophSlot.getContainerSlot() + " slotClass=" + sophSlot.getClass().getSimpleName()
                + " container=" + sophSlot.container.getClass().getSimpleName();
        }
        DiagLog.lineThrottled("soph.limit." + counted,
            "[diag] soph limit: counted=" + counted + " limit=" + limit + " unlocked=" + unlocked + " " + where);
        if (!unlocked && counted >= limit + 1L) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "m_7648_(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
        at = @At("RETURN"), require = 0)
    private void ultrastacks$recordRun(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack moved = cir.getReturnValue();
        if (moved != null && !moved.isEmpty()) {
            BatchCounter.record(player.getUUID());
        }
    }
}
