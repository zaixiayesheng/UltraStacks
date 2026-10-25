package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 药水槽跟着药水堆叠上限走。
 *
 * <p>原版在 {@code BrewingStandMenu$PotionSlot} 里把 {@code getMaxStackSize()} 固定成 1，与物品自己的上限
 * 无关，于是药水即便能叠 4096 个，酿造台的瓶位也只放得下一瓶。这里取「配置的药水上限」与「槽里已有数量」
 * 的较大者，并且只在比原版返回值更大时才改：数量不会因为改小上限而被槽位拒收。
 *
 * <p>Potion slots follow the configured potion limit.
 *
 * <p>Vanilla {@code BrewingStandMenu$PotionSlot} hardcodes {@code getMaxStackSize()} to 1 regardless of
 * the item's own limit, so even with potions stacking to 4096 a stand slot holds a single bottle. This
 * takes the larger of the configured potion limit and the number already sitting in the slot, and only
 * raises the value returned by vanilla — lowering the limit can therefore never make a slot reject what
 * it already contains.
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
