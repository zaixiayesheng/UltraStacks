package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 精致存储的物品处理走 NeoForge 的 transfer 层，每格容量由 {@code getCapacityAsLong} 给出。
     *
 * <p>Refined Storage handles items through NeoForge's transfer layer, where the per-slot capacity comes from
 * {@code getCapacityAsLong}.
 */
@Mixin(targets = "com.refinedmods.refinedstorage.neoforge.support.inventory.InsertExtractResourceHandler",
        remap = false)
public abstract class RefinedStorageResourceHandlerMixin {

    @ModifyReturnValue(method = "getCapacityAsLong(ILnet/neoforged/neoforge/transfer/resource/Resource;)J",
            at = @At("RETURN"), require = 0)
    private long ultrastacks$raiseCapacity(long original) {
        return UltraStacksConfig.refinedStorageCapacity(original);
    }
}
