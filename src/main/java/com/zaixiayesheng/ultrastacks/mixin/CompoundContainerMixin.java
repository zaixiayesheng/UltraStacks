package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.CompoundContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 大箱子与其它复合容器。
 *
 * <p>这类容器把上限查询转给内部包着的那几个容器，本方法直接返回配置的堆叠上限。
 *
 * <p>Double chests and other compound containers delegate the limit query to the containers they wrap;
 * this returns the configured stack size instead.
 */
@Mixin(CompoundContainer.class)
public abstract class CompoundContainerMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.maxStackSize());
    }
}