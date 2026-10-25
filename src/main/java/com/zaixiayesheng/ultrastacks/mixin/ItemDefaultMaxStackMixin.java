package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 物品自带的默认堆叠上限也跟着配置走。
 *
 * <p>{@code getDefaultMaxStackSize()} 的值在物品注册时就固定了，而本模组主要改的是 {@code ItemStack} 的
 * 默认组件；只改后者的话，凡是直接问「这个物品最多叠几个」的代码拿到的仍是 64。这里按
 * {@link com.zaixiayesheng.ultrastacks.UltraStacksConfig#raiseSlotLimit(int)} 的规则，把 64 与 99 换成
 * 配置值，其它数值原样返回。
 *
 * <p>The default stack size an item reports follows the config as well.
 *
 * <p>{@code getDefaultMaxStackSize()} is fixed when the item is registered, while this mod mainly patches
 * the default components of {@code ItemStack}; with only the latter changed, any code that asks the item
 * itself "how many of these fit in one stack" still gets 64. This applies the rule of
 * {@link com.zaixiayesheng.ultrastacks.UltraStacksConfig#raiseSlotLimit(int)}, replacing 64 and 99 with the
 * configured value and returning every other number unchanged.
 */
@Mixin(Item.class)
public class ItemDefaultMaxStackMixin {

    @Inject(method = "getDefaultMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseDefaultMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSlotLimit(cir.getReturnValue()));
    }
}
