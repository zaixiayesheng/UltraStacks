package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 抽屉潜行右键只取一组。
 *
 * <p>原版 {@code ItemControllableDrawerTile.onClicked} 用 {@code getStackInSlot(slot).getMaxStackSize()}
 * 作为潜行取出的数量。显示栈的 count 等于槽内总存，而本模组的 {@code ItemStackMaxStackMixin} 会把上限
 * 抬到不低于 count，于是「一组」被算成整槽总量，shift 一次取光。这里把该调用点固定为配置的堆叠上限
 * （一组 = 65536），恢复原版「一次取一组」的手感。</p>
 *
 * <p>Sneak-click takes one raised stack. Vanilla {@code ItemControllableDrawerTile.onClicked} uses
 * {@code getStackInSlot(slot).getMaxStackSize()} as the sneak-extract amount; the slot's display
 * stack count equals the stored total, and our {@code ItemStackMaxStackMixin} raises the limit to at
 * least that count, so "one stack" becomes the whole slot. This redirect pins the amount to the
 * configured stack limit (one stack = 65536).</p>
 */
@Mixin(targets = "com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile", remap = false)
public abstract class FunctionalStorageDrawerClickMixin {

    @Redirect(
            method = "onClicked(Lnet/minecraft/world/entity/player/Player;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"),
            require = 0
    )
    private int ultrastacks$shiftTakesOneStack(ItemStack stack) {
        return UltraStacksConfig.maxStackSize();
    }
}
