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
 * <p>很多机器的槽位上限是处理器给的，不走容器那一层；不拦这里，配置调大后这些槽位照样只吃 64 个。
 * 只升不降：处理器给的值比配置大时保持原值。{@code ItemStackHandler} 与 {@code getSlotLimit} 都是 Forge
 * 的类和方法，所以 {@code remap = false}。
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
