package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.items.ComponentItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 把物品组件式物品槽的每格上限抬到配置的堆叠上限。
 *
 * <p>{@code ComponentItemHandler} 是 NeoForge 提供的、把物品栏内容存在物品数据组件里的那套槽位实现，
 * 它的 {@code getSlotLimit} 返回值在这里交给
 * {@link com.zaixiayesheng.ultrastacks.UltraStacksConfig#raiseModdedSlotLimit(int)}：只升不降，模组自己
 * 设得更大的上限不会被压回去。
 *
 * <p>Raises the per-slot limit of component-backed item slots to the configured stack size.
 *
 * <p>{@code ComponentItemHandler} is NeoForge's slot implementation whose contents live in an item data
 * component; its {@code getSlotLimit} return value is passed to
 * {@link com.zaixiayesheng.ultrastacks.UltraStacksConfig#raiseModdedSlotLimit(int)}, which only raises —
 * a larger limit set by the owning mod is never pushed back down.
 */
@Mixin(ComponentItemHandler.class)
public abstract class ComponentItemHandlerMixin {
    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int ultrastacks$raiseModdedLimit(int original) {
        return UltraStacksConfig.raiseModdedSlotLimit(original);
    }
}
