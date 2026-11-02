package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 合成辅助在精妙合成页点“清空网格”时，别按它自己的规矩塞玩家物品栏，改走精妙菜单的 {@code quickMoveStack}，
 * 这样产物/材料的归处完全跟随精妙（先背包存储、满了再进物品栏）。注入不上或异常时放行原逻辑。
 *
 * <p>When CraftingTweaks clears the grid inside a Sophisticated crafting tab, route the stacks through the
 * Sophisticated menu's quick move so the destination matches its own rules. Any failure falls back to the
 * mod's original behaviour.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks.CraftingUpgradeTweakProvider", remap = false)
public abstract class SophisticatedClearGridMixin {

    @Inject(method = "clearGrid(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;Z)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$clearGridViaMenu(Player player, AbstractContainerMenu menu, boolean transferToPlayer,
            CallbackInfo ci) {
        try {
            int moved = 0;
            for (Slot slot : menu.slots) {
                if (slot.container == null || slot.getItem().isEmpty()) {
                    continue;
                }
                String container = slot.container.getClass().getName();
                if (!container.contains("Crafting") && !container.contains("crafting")) {
                    continue;
                }
                menu.quickMoveStack(player, slot.index);
                moved++;
            }
            if (moved > 0) {
                menu.broadcastChanges();
                DiagLog.lineThrottled("cleargrid." + moved, "[diag] clear grid via menu: moved=" + moved);
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // 出任何问题就交给合成辅助自己处理 / fall back to the mod's own logic
        }
    }
}
