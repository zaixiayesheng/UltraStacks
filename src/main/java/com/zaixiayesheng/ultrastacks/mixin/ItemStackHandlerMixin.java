package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge 的 ItemStackHandler。
 *
 * <p>机器槽位常用它，默认槽位上限是 64，
 * 而且只有配置里打开 raiseModdedSlotLimits 才替换；只把「正好 64 / 99」的老默认值换掉，
 * 别的模组显式设的 1、16、1024 保持原样。
 *
 * <p>ItemStackHandler 是 Forge 的类，不在 Minecraft 映射里，所以 remap = false。
 *
 * <p><b>EN</b> Raises ItemStackHandler slot limits to the configured value and never lowers them.
 */
@Mixin(value = ItemStackHandler.class, remap = false)
public abstract class ItemStackHandlerMixin {
    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseModdedLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseLegacyDefault(cir.getReturnValue()));
    }
}
