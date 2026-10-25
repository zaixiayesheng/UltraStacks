package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙「合成升级」的合成格也要跟着配置走。
 *
 * <p>那块合成格用的是它自己的 {@code StatefulComponentItemHandler}，每格固定报 99，所以存储槽的上限抬上去
 * 之后，合成格仍然只放得下 99 个。合成格保留着自己的处理器，存储那边打的补丁到不了这里，只能单独补。规则
 * 与存储槽一致：把 64 与 99 换成配置上限，其它数值原样返回。
 *
 * <p>The crafting grid of the Sophisticated crafting upgrade needs to follow the config as well.
 *
 * <p>That grid uses its own {@code StatefulComponentItemHandler}, which reports 99 for every slot, so even
 * after the storage slot limits were raised a crafting slot still only held 99 items. The grid keeps its own
 * handler, which the storage-side patch never reaches, so it is patched separately. The rule is the same as
 * for storage slots: 64 and 99 are replaced with the configured limit and any other value is passed through.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.StatefulComponentItemHandler", remap = false)
public abstract class SophisticatedStatefulComponentItemHandlerMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }
}
