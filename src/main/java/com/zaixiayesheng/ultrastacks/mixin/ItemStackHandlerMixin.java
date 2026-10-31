package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 来自模组物品处理器的槽位上限。
     *
 * <p>Slot limits that come from a mod's item handler. Many machines take their slot limit from the
 * handler rather than the container, and without this injection those slots still hold 64 after the
 * limit is raised. Only the hardcoded 64 and 99 are lifted: a larger handler limit wins.
 * {@code ItemStackHandler} and {@code getSlotLimit} are Forge members, so the mixin uses
 * {@code remap = false}.
 */
@Mixin(value = ItemStackHandler.class, remap = false)
public abstract class ItemStackHandlerMixin {
    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseModdedLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseLegacyDefault(cir.getReturnValue()));
    }
}
