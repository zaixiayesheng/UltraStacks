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
 * <p>The default stack size an item reports follows the config as well.
 */
@Mixin(Item.class)
public class ItemDefaultMaxStackMixin {

    @Inject(method = "getDefaultMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseDefaultMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSlotLimit(cir.getReturnValue()));
    }
}
