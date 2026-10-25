package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 物品自身的默认堆叠上限也跟随配置。
 *
 * <p>{@code Item#getDefaultMaxStackSize()} 读的是物品自身的默认组件，而不是某个物品栈上的组件；
 * 本模组改的 {@code MAX_STACK_SIZE} 未必经过这条路，所以这里按与槽位相同的规则补一遍：64 和 99
 * 视为「没配置过」换成配置上限，其他数字（例如 16、1）保持原值。若该物品的组件已经是新上限，
 * 这里自然不再变化。
 *
 * <p>Raises the item-level default stack size as well. {@code Item#getDefaultMaxStackSize()} reads
 * the item's own default components rather than the components of a particular stack, so this hook
 * applies the same rule as the slot hooks: 64 and 99 count as "not configured" and are replaced
 * with the configured limit, while other numbers such as 16 or 1 are kept. If the item's components
 * already carry the new limit, the value simply does not change.
 */
@Mixin(Item.class)
public class ItemDefaultMaxStackMixin {

    @Inject(method = "getDefaultMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseDefaultMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSlotLimit(cir.getReturnValue()));
    }
}
