package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 物品槽处理器（{@code ItemStackHandler}）的槽位上限。
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
