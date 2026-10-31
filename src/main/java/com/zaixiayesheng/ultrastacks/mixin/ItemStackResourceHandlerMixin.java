package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStackResourceHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * transfer 层单堆处理器的容量：原版是 min(物品上限, 99)，这里把 99 和 64 换成配置值。
 * 只升不降，工具盔甲那种 1 保持原样。
     *
 * Transfer-layer capacity: vanilla caps it at min(item limit, 99); swap 99 and 64
 * for the configured value. Never lowered; the 1 used by tools stays.
 */
@Mixin(ItemStackResourceHandler.class)
public abstract class ItemStackResourceHandlerMixin {

    @ModifyReturnValue(method = "getCapacity", at = @At("RETURN"))
    private int ultrastacks$raiseCapacity(int original, ItemResource resource) {
        return UltraStacksConfig.raiseSlotLimit(original);
    }
}
