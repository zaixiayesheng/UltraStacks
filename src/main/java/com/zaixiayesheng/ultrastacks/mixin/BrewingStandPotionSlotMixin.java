package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 酿造台的三个药水槽每格只放一瓶的问题。
 *
 * <p>原版在 {@code PotionSlot} 里重写了 {@code getMaxStackSize()} 固定返回 1，物品本身能堆多大都没用。
 * 这里改成跟随药水堆叠上限，并且不小于槽内已有数量，避免读档或配置改小后槽里的数量被判成超限。
 *
 * <p>The three potion slots of a brewing stand hold a single bottle each. Vanilla overrides
 * {@code getMaxStackSize()} in {@code PotionSlot} to return one regardless of what the item allows;
 * the slot now follows the potion limit and never reports less than the count already in it, so a
 * lowered config or a reloaded chunk cannot put the slot in an over-limit state.
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
