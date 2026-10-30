package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 点一下能并进去多少。
 *
 * <p>原版 doClick 在「同一种东西、槽里还放得下」的时候，把这次要并的量写死成槽里现有的数量：
 * {@code amount = slotStack.getCount()}，然后 {@code safeInsert(stack, amount)}。64 的时代这数永远够用，
 * 两边最多也就 64。抬到 65536 就不行了——手里 10944 去点槽里 7744，它只让你并 7744，看着就像堆不起来。
 * 这里改成要多少给多少，真正的夹取还是 safeInsert 自己在做，不会凭空变东西出来。</p>
 *
 * <p>Vanilla hard-codes the merge amount as the slot's current count. Fine while both sides cap at 64;
 * broken once the limit is 65536, where 10944 onto 7744 only moves 7744. We ask for everything and let
 * safeInsert clamp as usual.</p>
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
