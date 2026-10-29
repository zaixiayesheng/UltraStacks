package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 漏斗槽位的上限。
 *
 * <p>{@code HopperBlockEntity.setItem} 用容器默认上限（64）夹取进来的物品，这里换成配置的堆叠上限。
 *
 * <p>Hopper slot limit. {@code HopperBlockEntity.setItem} clamps incoming items to the container default
 * of 64; this redirects that call to the configured stack size.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Redirect(
            method = "setItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getMaxStackSize()I"),
            require = 0
    )
    private int ultrastacks$limitForSetItem(HopperBlockEntity hopper) {
        return UltraStacksConfig.maxStackSize();
    }
}