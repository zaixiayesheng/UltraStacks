package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 精致存储的物品处理走 NeoForge 的 transfer 层，每格容量由 {@code getCapacityAsLong} 给出。
 *
 * <p>物品资源在那里报 64，整摞东西存进网络时会被截断成一格 64 个。这里按 {@code refinedStorageCapacity}
 * 把 64 与 99 换成配置上限，其它数值原样返回，兼容开关关闭时不生效。
 *
 * <p>Refined Storage handles items through NeoForge's transfer layer, where the per-slot capacity comes from
 * {@code getCapacityAsLong}.
 *
 * <p>Item resources report 64 there, so a whole stack put into the network is cut down to 64 per slot. The
 * two vanilla-sized values, 64 and 99, are replaced with the configured limit by
 * {@code refinedStorageCapacity}, any other value is passed through, and the whole conversion is skipped
 * while the compat switch is off.
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
