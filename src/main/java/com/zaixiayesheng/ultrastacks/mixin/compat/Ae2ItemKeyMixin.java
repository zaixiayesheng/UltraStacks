package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 键里缓存的堆叠上限跟着配置走。
 *
 * <p>{@code AEItemKey} 会把堆叠上限存进键里，键若在本模组抬高上限之前就已建好，之后从终端取出的堆叠会卡在
 * 旧值上。这里只抬 {@code getMaxStackSize} 的返回值，存储里的实际数量一概不动。
 *
 * <p>The stack limit cached inside an item key follows the config.
 *
 * <p>{@code AEItemKey} stores a stack limit in the key itself, and a key built before this mod raised the
 * limit keeps the old value, so stacks taken out of a terminal stay capped. Only the return value of
 * {@code getMaxStackSize} is raised; the amount actually stored is left completely untouched.
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
