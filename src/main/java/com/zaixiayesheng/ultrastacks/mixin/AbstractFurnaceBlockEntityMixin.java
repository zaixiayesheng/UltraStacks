package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla furnace limit.
 *
 * <p>{@code setItem} clamps to the container default (64) and the tick path uses that value as the
 * per-smelt output cap, so a 32768 stack could never sit in a furnace slot. Both call sites are
 * redirected to the configured limit.
 *
 * <p><b>中文</b> 1.20.1 熔炉走容器层夹取，这里改到配置上限。
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Redirect(
            method = "setItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getMaxStackSize()I"),
            require = 0
    )
    private int ultrastacks$limitForSetItem(AbstractFurnaceBlockEntity furnace) {
        return UltraStacksConfig.maxStackSize();
    }

    @Redirect(
            method = "serverTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getMaxStackSize()I"),
            require = 0
    )
    private static int ultrastacks$limitForTick(AbstractFurnaceBlockEntity furnace) {
        return UltraStacksConfig.maxStackSize();
    }
}