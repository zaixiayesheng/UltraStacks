package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge 容器包装器的槽位上限。
 *
 * <p>{@code InvWrapper} 把 {@code getSlotLimit} 直接转给容器的 {@code getMaxStackSize()}，玩家背包这类
 * 容器返回 64，于是别的模组通过物品处理器搬东西时每格仍只吃 64 个，即使界面上的槽位已经放开。
 * 这里只把 64 与 99 这两种写死的值抬到配置上限，容器给得更大的保持原值。
 *
 * <p>{@code InvWrapper} 与 {@code getSlotLimit} 都是 Forge 的类和方法，不在 SRG 映射里，所以整个 mixin
 * 用 {@code remap = false}。
 *
 * <p>Slot limit of the Forge container wrapper. {@code InvWrapper} forwards {@code getSlotLimit} to the
 * wrapped container's {@code getMaxStackSize()}; for containers such as the player inventory that means
 * 64, so another mod moving items through an item handler still gets 64 per slot even after the GUI slots
 * are open. Only the hardcoded 64 and 99 are raised to the configured limit, and a larger value from the
 * container is kept. Both {@code InvWrapper} and {@code getSlotLimit} are Forge members outside the SRG
 * mappings, so the mixin is declared with {@code remap = false}.
 */
@Mixin(value = InvWrapper.class, remap = false)
public abstract class InvWrapperMixin {
    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseHandlerLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseLegacyDefault(cir.getReturnValue()));
    }
}
