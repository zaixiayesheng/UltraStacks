package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.items.ComponentItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge 组件物品槽（{@code ComponentItemHandler}）的槽位上限。
 *
 * <p>它的默认实现让 {@code getSlotLimit} 返回 {@code Item.ABSOLUTE_MAX_STACK_SIZE}，即 99，
 * 所以物品本身能堆得再高，插进这种槽位也会被卡在 99。这里换成 {@code max(99, 配置上限)}，
 * 只升不降：已经放在槽里的更大堆叠不会因为上限变小而失效。
 *
 * <p>Slot limit for NeoForge's item-component backed handler {@code ComponentItemHandler}. Its
 * default implementation returns {@code Item.ABSOLUTE_MAX_STACK_SIZE}, which is 99, so an item
 * that stacks higher is still capped at 99 when inserted. The hook substitutes
 * {@code max(99, configured limit)} and never lowers the value, so a larger stack already in the
 * slot stays valid.
 */
@Mixin(ComponentItemHandler.class)
public abstract class ComponentItemHandlerMixin {
    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int ultrastacks$raiseModdedLimit(int original) {
        return UltraStacksConfig.raiseModdedSlotLimit(original);
    }
}
