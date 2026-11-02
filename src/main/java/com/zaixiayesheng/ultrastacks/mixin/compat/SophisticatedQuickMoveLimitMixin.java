package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.crafting.BatchCounter;
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
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class SophisticatedQuickMoveLimitMixin {
    @Inject(method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$capRuns(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        // 精妙在进入自己的循环前会先合成一次，那一次不经过这里；所以这条路径多放一次，
        // 使「循环外的 1 次 + 循环里的 N 次」正好等于配置的上限。
        // Sophisticated crafts once before entering its own loop, which never reaches this hook, so this
        // path allows exactly one extra move: the outside craft plus the loop equals the configured cap.
        // 一次性探针：确认合成辅助的精妙接入类在客户端是否被加载。
        com.zaixiayesheng.ultrastacks.DiagLog.lineThrottled("probe.crafttweaks",
                "[diag] crafttweaks provider exists=" + com.zaixiayesheng.ultrastacks.crafting.ClassProbe.exists(
                        "net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks.CraftingUpgradeTweakProvider")
                        + " index=" + index);
        boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey()
            && com.zaixiayesheng.ultrastacks.crafting.UnlockState.isUnlocked(player);
        long limit = Math.max(1, UltraStacksConfig.bulkCraftLimit());
        long counted = BatchCounter.count(player.getUUID());
        com.zaixiayesheng.ultrastacks.DiagLog.lineThrottled("soph.limit." + counted,
            "[diag] soph limit: counted=" + counted + " limit=" + limit);
        if (!unlocked && counted >= limit + 1L) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
        at = @At("RETURN"), require = 0)
    private void ultrastacks$recordRun(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack moved = cir.getReturnValue();
        if (moved != null && !moved.isEmpty()) {
            BatchCounter.record(player.getUUID());
        }
    }
}
