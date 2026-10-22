package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge 的 ItemStackHandler（很多模组的机器槽位用它）。
 * 只有配置里打开 raiseModdedSlotLimits 才生效，而且只升不降。
 *
 * <p><b>EN</b> Raises ItemStackHandler slot limits to the configured value and never lowers them.
 */
@Mixin(ItemStackHandler.class)
public abstract class ItemStackHandlerMixin {

    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int ultrastacks$raiseModdedLimit(int original) {
        return UltraStacksConfig.raiseModdedSlotLimit(original);
    }
}
