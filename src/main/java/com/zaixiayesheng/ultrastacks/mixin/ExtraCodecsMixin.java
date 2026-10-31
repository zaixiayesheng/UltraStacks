package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 把 intRange(1, 99) 的上界放到硬上限，不然超过 99 的 count 在读档时会被判越界。
 * 用编译期常量而不是配置值——这些 codec 是静态字段，建得比配置加载还早。
     *
 * Widens the intRange(1, 99) upper bound, otherwise counts above 99 fail the codec
 * on load. Uses a compile-time constant because these codecs are built before config load.
 */
@Mixin(ExtraCodecs.class)
public abstract class ExtraCodecsMixin {
    @ModifyVariable(method = "intRange", at = @At("HEAD"), argsOnly = true, index = 1)
    private static int ultrastacks$raiseUpperBound(int bound) {
        return UltraStacksConfig.raiseCodecUpperBound(bound);
    }
}
