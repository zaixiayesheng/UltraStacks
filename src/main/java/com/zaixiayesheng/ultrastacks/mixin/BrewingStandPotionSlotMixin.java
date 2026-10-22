package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the three potion slots hold a whole stack.
 *
 * <p>Vanilla overrides {@code getMaxStackSize()} in this slot to return one, which caps the brewing
 * stand no matter what the item itself allows. The slot now follows the configured potion limit and
 * never reports less than what already sits in it.
 *
 * <p><b>中文</b> 原版在药水槽里重写 {@code getMaxStackSize()} 固定返回 1，导致酿造台无论物品本身能堆多大
 * 都只能放一瓶。这里改为跟随药水堆叠上限，并保证不小于槽内已有数量。
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
