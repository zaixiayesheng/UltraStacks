package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 潜行右键抽屉，就该只拿一组。
 *
 * <p>原版拿 {@code getStackInSlot(slot).getMaxStackSize()} 当取出量。问题是显示栈的 count 就是槽里
 * 的总量，我们的 {@code ItemStackMaxStackMixin} 又把上限抬到不低于 count——问出来直接是整槽，
 * shift 一下抽屉就空了。所以这儿固定成配置的上限，一组就是 65536。</p>
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
