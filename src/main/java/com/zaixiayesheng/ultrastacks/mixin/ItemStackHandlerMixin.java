package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 给模组自己的槽位处理器抬上限。
 *
 * <p>不少机器、背包的每格上限来自 {@code ItemStackHandler#getSlotLimit} 而不是容器，这一层不拦，配置调到
 * 65536 之后那些槽位仍停在 99（这是 NeoForge 该方法的默认返回值）。只升不降：处理器自己给出更大的值时
 * 保持原值。
 *
 * <p>Raises the per-slot limit of modded item handlers.
 *
 * <p>Plenty of machines and backpacks get their per-slot limit from {@code ItemStackHandler#getSlotLimit}
 * rather than from a container, and without hooking that layer those slots stay at 99 — the default return
 * value of the NeoForge method — no matter how high the config goes. Only raises: a larger value supplied by
 * the handler itself is kept.
 */
@Mixin(ItemStackHandler.class)
public abstract class ItemStackHandlerMixin {

    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int ultrastacks$raiseModdedLimit(int original) {
        return UltraStacksConfig.raiseModdedSlotLimit(original);
    }
}
