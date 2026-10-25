package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 熔炉槽位只肯收 64 个的问题。
 *
 * <p>{@code setItem} 用容器默认上限（64）夹取入炉物品，{@code serverTick} 又拿同一个值当成一次烧炼
 * 允许的产出去向上限，于是大堆叠连放进熔炉格都做不到。两处调用一起重定向到配置的堆叠上限。
 *
 * <p>Furnace slots stop at 64 because {@code setItem} clamps incoming items to the container default
 * and {@code serverTick} reuses that value as the per-smelt output cap, so a large stack could not even
 * enter the slot. Both call sites are redirected to the configured stack size.
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