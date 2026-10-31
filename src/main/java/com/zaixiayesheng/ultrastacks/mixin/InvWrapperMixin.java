package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版物品栏经 {@code InvWrapper} 暴露给物品处理器时，每格上限取 {@code Container#getMaxStackSize()}（64），整理模组据此认为一格只能放 64 个。
     *
 * <p>When the vanilla inventory is exposed through {@code InvWrapper}, each slot reports
 * {@code Container#getMaxStackSize()} (64), so sorting mods believe a slot only holds 64 items.
 * Only 64 and 99 are raised to the configured limit here, keeping handler-based paths (sorting,
 * hoppers, pipes) in line with the slot limits.
 */
@Mixin(targets = "net.neoforged.neoforge.items.wrapper.InvWrapper", remap = false)
public abstract class InvWrapperMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseVanillaDefault(cir.getReturnValueI()));
    }
}
