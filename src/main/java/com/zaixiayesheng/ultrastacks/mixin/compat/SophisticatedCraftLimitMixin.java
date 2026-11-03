package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙合成页的“一次 Shift 做多少次”跟着我们的批量设定走：不按解锁键最多 {@code bulkCraftLimit} 个产物，
 * 按住解锁键才做到底。
 *
 * <p>The Sophisticated crafting tab repeats until {@code bulkCraftLimit} items unless the unlock key is
 * held, which is the same rule the vanilla crafting menus use.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer", remap = false)
public abstract class SophisticatedCraftLimitMixin {
    @Inject(method = "getRepeatedQuickMoveLimit(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/item/ItemStack;)I",
        at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$capRepeats(Slot slot, ItemStack transferred, CallbackInfoReturnable<Integer> cir) {
        if (!UltraStacksConfig.craftingModule() || !UltraStacksConfig.bulkCrafting()) {
            return;
        }
        if (UltraStacksConfig.bulkCraftUnlockKey() && com.zaixiayesheng.ultrastacks.crafting.UnlockState.currentUnlocked()) {
            // 解锁键：不设上限，精妙自己怎么做就怎么做到底。
            // Unlocked: no cap at all, let Sophisticated run its own loop to the end.
            cir.setReturnValue(Integer.MAX_VALUE);
            return;
        }
        // 上限按“配方次数”算，和原版工作台完全一致：一次 Shift 做 bulkCraftLimit 次，
        // 产物总数 = 配方单次产出 × 次数（铁块→9 锭时就是 9 × 64）。
        // The cap counts recipe runs, exactly like the vanilla crafting menus: one Shift does
        // bulkCraftLimit runs, so the produced amount is output-per-craft × runs.
        cir.setReturnValue(Math.min(cir.getReturnValue(), Math.max(1, UltraStacksConfig.bulkCraftLimit())));
    }
}
