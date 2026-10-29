package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 精致存储从终端往外取物品时的默认数量。
 *
 * <p>默认一次取 64 个是写死的，最终数量又取 {@code min(64, 槽位上限)}，所以槽位上限抬到 65536 也看不出
 * 变化。这里只换掉那个 64；右键取一半的分支仍然走原版的上限减半。
 *
 * <p>The default amount Refined Storage pulls out of a grid. It is a hardcoded 64, and the final amount is
 * {@code min(64, slot limit)}, so a slot limit raised to 65536 never shows up. Only that 64 is replaced
 * here; the right-click half branch still takes half of the vanilla limit.
 */
@Mixin(targets = "com.refinedmods.refinedstorage.apiimpl.network.grid.handler.ItemGridHandler", remap = false)
public abstract class RefinedStorageGridExtractMixin {

    @ModifyConstant(
            method = "onExtract(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/UUID;II)V",
            constant = @Constant(intValue = 64),
            require = 0)
    private int ultrastacks$raiseExtractAmount(int original) {
        return UltraStacksConfig.refinedStorageSlotLimit(original);
    }
}
