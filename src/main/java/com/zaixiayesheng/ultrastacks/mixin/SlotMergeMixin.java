package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 点击合并量。
 *
 * <p>原版 {@code AbstractContainerMenu.doClick} 在「手里拿着的东西和槽里是同一种、且槽里放得下」时，
 * 把一次点击能并进去的量写死成<b>槽里当前的数量</b>：{@code amount = slotStack.getCount()}，然后
 * {@code slot.safeInsert(stack, amount)}。上限是 64 的时候这个数永远够用——手里最多 64 个、槽里最多 64 个，
 * 一次点击总能把手里那堆全并进去。
 *
 * <p>上限抬到 65536 之后这条经验失效：手里 10944 个去点槽里 7744 个，原版只要求并入 7744 个，看起来就是
 * 「点一下只并进去一点点」「堆不起来」。这里把这次调用的请求量改成 {@link Integer#MAX_VALUE}，
 * 真正的上限仍由 {@code Slot.safeInsert} 自己按「槽上限 − 槽内数量」和「手里的数量」夹住，语义不变。
 *
 * <p>The merge amount of a click. Vanilla writes {@code amount = slotStack.getCount()} before calling
 * {@code slot.safeInsert(stack, amount)}, which is always enough while the limit is 64 because both sides
 * hold at most 64. With the limit raised to 65536 the assumption breaks: clicking 10944 onto 7744 only
 * asks to move 7744, which reads as "it barely merges". This redirects that one call to
 * {@link Integer#MAX_VALUE}; {@code Slot.safeInsert} still clamps to the real headroom and to the carrier.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class SlotMergeMixin {

    @Redirect(
            method = "doClick(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;"),
            require = 0)
    private ItemStack ultrastacks$mergeWholeStack(Slot slot, ItemStack stack, int amount) {
        return slot.safeInsert(stack, Integer.MAX_VALUE);
    }
}
