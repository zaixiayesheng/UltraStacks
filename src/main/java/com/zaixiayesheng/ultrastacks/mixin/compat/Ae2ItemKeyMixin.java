package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code AEItemKey} 自带一个堆叠上限，本模组在键之外抬高的值不会自动反映到这里。
 *
 * <p>只抬 {@code getMaxStackSize} 的返回值：64 与 99 换成 {@link UltraStacksConfig#ae2SlotLimit(int)}，其它值保持；键里记录的数量不动。
 *
 * <p>{@code AEItemKey} carries a stack limit of its own, and a limit raised outside the key is not
 * reflected here automatically.
 *
 * <p>Only the return value of {@code getMaxStackSize} is raised: 64 and 99 become
 * {@link UltraStacksConfig#ae2SlotLimit(int)} and every other value is kept; the amount stored in the
 * key is left alone.
 */
@Mixin(targets = "appeng.api.stacks.AEItemKey", remap = false)
public abstract class Ae2ItemKeyMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.ae2SlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
