package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.SimpleContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Generic container limit (hoppers, dispensers, chest minecarts, ...).
 *
 * <p>{@code SimpleContainer.setItem} clamps the incoming stack to {@code getMaxStackSize()}
 * (64 by default on 1.20.1); the call is redirected to the configured limit.
 *
 * <p><b>中文</b> SimpleContainer 插入时按自身上限夹取，这里抬到配置值。
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