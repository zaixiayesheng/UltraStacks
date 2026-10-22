package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code ItemStackHandler}, the slot storage many machines use.
 *
 * <p>The limit is only raised when {@code raiseModdedSlotLimits} is on, and it is never lowered: the
 * configured value replaces the handler's own limit only when that limit is smaller.
 *
 * <p><b>中文</b> NeoForge 的 {@code ItemStackHandler} 是很多机器用来存放槽位的实现。只有打开
 * {@code raiseModdedSlotLimits} 才会生效，而且只升不降：处理器自己的上限比配置小时才替换成配置值。
 */
@Mixin(ItemStackHandler.class)
public abstract class ItemStackHandlerMixin {

    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int ultrastacks$raiseModdedLimit(int original) {
        return UltraStacksConfig.raiseModdedSlotLimit(original);
    }
}
