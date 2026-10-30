package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 临时探针：看看 AE 终端往网络塞东西、从网络拿东西时到底报了多少。
 *
 * <p>{@code transferStackToMenu} 的入参就是玩家手里那一堆（含数量），返回值是网络实际收下的数量；
 * 两个值对不上就说明网络侧还有一道夹取。取出量另有一条探针挂在 {@code AEItemKey.getMaxStackSize()}
 * 上——AE2 从网络取东西时取多少完全由那个数决定。</p>
 *
 * <p>Temporary probe for the AE terminal: what a deposit asks for versus what the network accepts,
 * so a mismatch points at a clamp inside the network. Withdrawals are measured separately on
 * {@code AEItemKey.getMaxStackSize()}, which is the exact amount AE2 pulls out of the network.</p>
 */
@Mixin(targets = "appeng.menu.me.common.MEStorageMenu", remap = false)
public abstract class Ae2TerminalDiagMixin {

    @Inject(method = "transferStackToMenu(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), require = 0)
    private void ultrastacks$logDeposit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        DiagLog.line("[diag] ae2 deposit: " + stack.getDescriptionId() + " x" + stack.getCount()
                + " accepted=" + cir.getReturnValue() + " itemMax=" + stack.getMaxStackSize());
    }
}
