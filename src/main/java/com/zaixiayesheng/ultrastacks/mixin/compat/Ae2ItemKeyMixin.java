package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 把 AEItemKey.getMaxStackSize() 当“一次能搬多少”用，可我们把 ItemStack 的上限抬到不小于自身数量，
 * 于是从 8479 那堆建出来的 key 就报 8479，终端一次只搬这么多。这里改用物品自己的默认上限。
     *
 * AE2 reads AEItemKey.getMaxStackSize() as "how much per operation", but this mod makes that method
 * report at least the stack's own count — a key built from 8479 moved 8479 per operation. Rebuilt
 * from the item's default limit instead.
 */
@Mixin(targets = "appeng.api.stacks.AEItemKey", remap = false)
public abstract class Ae2ItemKeyMixin {

    @Shadow
    public abstract ItemStack getReadOnlyStack();

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        int byItem = getReadOnlyStack().getItem().getDefaultMaxStackSize();
        int raised = Math.max(UltraStacksConfig.ae2SlotLimit(original), byItem);
        if (raised != original) {
            if (original != 64 && original != 99) {
                DiagLog.lineThrottled("ae2-key-max-" + original,
                        "[diag] ae2 key max: " + original + " -> " + raised + " (item default " + byItem + ")");
            }
            cir.setReturnValue(raised);
        }
    }
}
