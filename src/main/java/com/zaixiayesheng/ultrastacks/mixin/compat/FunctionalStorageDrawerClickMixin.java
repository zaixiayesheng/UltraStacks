package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 潜行右键抽屉，就该只拿一组。
     *
 * <p>Vanilla asks {@code getStackInSlot(slot).getMaxStackSize()} for the sneak amount, but the
 * display stack's count is the whole slot and our mixin raises the limit to match it — so "one
 * stack" meant "everything". Pinned to the configured limit instead.</p>
 */
@Mixin(targets = "com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile", remap = false)
public abstract class FunctionalStorageDrawerClickMixin {

    /**
     * 一组 = 配置上限。不能让它自己去问 getMaxStackSize()，那个值早被抬到「不低于槽内数量」了。
     *
     * <p>One stack = the configured limit. Can't let it ask getMaxStackSize(): we already raised
     * that answer to at least the stored count.</p>
     */
    @Redirect(
            method = "onClicked(Lnet/minecraft/world/entity/player/Player;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"),
            require = 0
    )
    private int ultrastacks$shiftTakesOneStack(ItemStack stack) {
        return UltraStacksConfig.maxStackSize();
    }
}
