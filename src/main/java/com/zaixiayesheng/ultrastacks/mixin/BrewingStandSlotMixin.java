package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 酿造台的燃料槽与原料槽。
 *
 * <p>这两个槽自己重写了 {@code getMaxStackSize()} 并直接返回 64，走不到 {@code SlotMixin} 那层注入；
 * 而 {@code AbstractContainerMenu.moveItemStackTo} 取的是
 * {@code Math.min(slot.getMaxStackSize(), stack.getMaxStackSize())}，槽自己的 64 会先把物品卡住。
 * 所以在这里再按「模组写死的旧上限」抬一次。
 *
 * <p>The fuel and ingredient slots of a brewing stand override {@code getMaxStackSize()} and return 64
 * directly, so the injection in {@code SlotMixin} never sees them. {@code moveItemStackTo} uses
 * {@code Math.min(slot.getMaxStackSize(), stack.getMaxStackSize())}, and that 64 stops the stack before
 * the item's own limit matters, so the slot limit is raised here as a hardcoded legacy default.
 */
@Mixin(targets = {
    "net.minecraft.world.inventory.BrewingStandMenu$FuelSlot",
    "net.minecraft.world.inventory.BrewingStandMenu$IngredientsSlot"
})
public abstract class BrewingStandSlotMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseLegacyDefault(cir.getReturnValue()));
    }
}
