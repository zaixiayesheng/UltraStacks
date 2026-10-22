package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.CompoundContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Double chest / compound container: report the configured limit instead of the wrapped container default.
 *
 * <p><b>中文</b> 大箱子通过 CompoundContainer 报上限，这里抬到配置值。
 */
@Mixin(CompoundContainer.class)
public abstract class CompoundContainerMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.maxStackSize());
    }
}