package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 酿造台的燃料槽与原料槽。
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
