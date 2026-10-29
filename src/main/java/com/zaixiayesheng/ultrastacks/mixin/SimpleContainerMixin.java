package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.SimpleContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 通用容器（漏斗、发射器、箱子矿车等）的槽位上限。
 *
 * <p>{@code SimpleContainer.setItem} 用自身上限夹取进来的物品，默认 64，这里换成配置的堆叠上限。
 *
 * <p>Slot limit of generic containers such as hoppers, dispensers and chest minecarts.
 * {@code SimpleContainer.setItem} clamps incoming items to its own limit, 64 by default; the call is
 * redirected to the configured stack size.
 */
@Mixin(SimpleContainer.class)
public abstract class SimpleContainerMixin {

    @Redirect(
            method = "setItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/SimpleContainer;getMaxStackSize()I"),
            require = 0
    )
    private int ultrastacks$limitForSetItem(SimpleContainer container) {
        return UltraStacksConfig.maxStackSize();
    }
}