package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 物品槽处理器（{@code ItemStackHandler}）的槽位上限。
 *
 * <p>它的 {@code getSlotLimit} 默认返回 {@code Item.ABSOLUTE_MAX_STACK_SIZE}，即 99，而不是物品
 * 自身的上限；机器的槽位多半按它限制，于是配置调大之后仍然只吃 99。这里换成
 * {@code max(99, 配置上限)}，只升不降：处理器自己给了更大的值就保持原值。
 *
 * <p>Slot limit for item handlers ({@code ItemStackHandler}). Its {@code getSlotLimit} returns
 * {@code Item.ABSOLUTE_MAX_STACK_SIZE}, which is 99, rather than the item's own limit; machine slots
 * usually rely on it, so a raised configuration would still be capped at 99. The hook substitutes
 * {@code max(99, configured limit)} and never lowers the value: a larger limit supplied by the
 * handler itself wins.
 */
@Mixin(ItemStackHandler.class)
public abstract class ItemStackHandlerMixin {

    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int ultrastacks$raiseModdedLimit(int original) {
        return UltraStacksConfig.raiseModdedSlotLimit(original);
    }
}
