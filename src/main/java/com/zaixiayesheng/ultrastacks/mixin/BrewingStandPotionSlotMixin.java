package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让酿造台的三个药水槽能放下整摞药水。
     *
 * <p>Lets the brewing stand's three potion slots hold a whole stack. Vanilla overrides
 * {@code getMaxStackSize()} in this slot to return one, so the item's own limit never helps. The
 * slot now follows the configured potion limit and never reports less than what already sits in
 * it, so taking part of a stack cannot leave the slot unable to hold the rest.
 */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public abstract class BrewingStandPotionSlotMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$potionSlotLimit(CallbackInfoReturnable<Integer> cir) {
        Slot self = (Slot) (Object) this;
        int limit = Math.max(UltraStacksConfig.potionStackLimit(), self.getItem().getCount());
        if (limit > cir.getReturnValueI()) {
            cir.setReturnValue(limit);
        }
    }
}
