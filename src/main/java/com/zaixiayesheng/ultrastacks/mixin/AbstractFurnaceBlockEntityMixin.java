package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 熔炉槽位只肯收 64 个的问题。
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