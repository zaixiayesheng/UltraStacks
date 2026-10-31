package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 点一下能并进去多少。
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
        // 右键（button 1）交还原版：那里 amount 是 1，把它改成 MAX 会让「放一个」变成「放一整摞」。
        if (ClickButtonContext.peek() != 0 || !slot.hasItem()) {
            return slot.safeInsert(stack, amount);
        }
        return slot.safeInsert(stack, Integer.MAX_VALUE);
    }

    @Inject(method = "doClick(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"), require = 0)
    private void ultrastacks$rememberButton(int slotId, int button, net.minecraft.world.inventory.ClickType type,
                                            Player player, CallbackInfo ci) {
        ClickButtonContext.push(button);
    }

    @Inject(method = "doClick(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("RETURN"), require = 0)
    private void ultrastacks$forgetButton(int slotId, int button, net.minecraft.world.inventory.ClickType type,
                                          Player player, CallbackInfo ci) {
        ClickButtonContext.clear();
    }
}
