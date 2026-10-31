package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 便携终端的取物数量。
     *
 * <p>The pull amount of the portable grid. It uses its own handler, which repeats the hardcoded 64-item
 * pull, and is patched the same way as the regular grid.
 */
@Mixin(targets = "com.refinedmods.refinedstorage.apiimpl.network.grid.handler.PortableItemGridHandler", remap = false)
public abstract class RefinedStoragePortableGridExtractMixin {

    @ModifyConstant(
            method = "onExtract(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/UUID;II)V",
            constant = @Constant(intValue = 64),
            require = 0)
    private int ultrastacks$raiseExtractAmount(int original) {
        return UltraStacksConfig.refinedStorageSlotLimit(original);
    }
}
