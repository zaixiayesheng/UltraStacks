package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 酿造台里写死 64 的两个槽（燃料槽、原料槽）。
 *
 * <p>它们重写了无参 {@code getMaxStackSize} 并直接返回 64，绕过 {@code SlotMixin} 的注入；
 * 而 {@code AbstractContainerMenu.moveItemStackTo} 用的是带物品参数之外的那条路径
 * （{@code Math.min(slot.getMaxStackSize(), stack.getMaxStackSize())}），所以只能在这里再抬一次。
 *
 * <p><b>EN</b> Brewing stand fuel and ingredient slots hardcode 64; this raises them to the configured limit.
 */
@Mixin(targets = {
    "net.minecraft.world.inventory.BrewingStandMenu$FuelSlot",
    "net.minecraft.world.inventory.BrewingStandMenu$IngredientsSlot"
})
public abstract class BrewingStandSlotMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSlotLimit(cir.getReturnValue()));
    }
}
