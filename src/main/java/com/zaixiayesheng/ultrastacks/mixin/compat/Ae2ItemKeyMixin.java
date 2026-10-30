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
 * 应用能源的物品键报的堆叠上限。
 *
 * <p>键的上限原本转的是「这一摞的 getMaxStackSize」，而本模组为了让大堆在槽里不显超容，会让它返回
 * 「至少等于当前数量」。键是从手里那摞建的，于是堆里有多少（比如 8479），上限就报多少 —— 终端一次
 * 只搬这么点，存取都像被截断。这里改成用物品自己的默认上限（已经在注册期按配置烘成 65536 / 16384），
 * 与手里有多少无关。只改返回值，键本身和存储里的数量都不动。
 *
 * <p>The stack limit an AE2 item key reports. The key forwards the limit of the stack it was built from,
 * and this mod keeps that getter at least as large as the stack's own count so oversized stacks do not
 * look over-capacity. A key built from a stack of 8479 therefore reports 8479, and a terminal moves that
 * much per operation. This uses the item's own default limit instead (already baked to 65536 / 16384 by
 * the config at registration), which has nothing to do with the count. Only the return value changes.
 */
@Mixin(targets = "appeng.api.stacks.AEItemKey", remap = false)
public abstract class Ae2ItemKeyMixin {

    @Shadow
    public abstract ItemStack getReadOnlyStack();

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        // 1.20.1 没有 Item.getDefaultMaxStackSize()，用物品自身声明的基础上限过一遍配置换算，
        // 得到的才是「一摞干净物品本该有的上限」，与手里有多少无关。
        ItemStack readOnly = getReadOnlyStack();
        int itemDefault = UltraStacksConfig.limitFor(readOnly.copyWithCount(1), readOnly.getItem().getMaxStackSize());
        int raised = Math.max(UltraStacksConfig.ae2SlotLimit(original), itemDefault);
        if (raised != original) {
            DiagLog.lineThrottled("ae2 key " + original + "->" + raised,
                    "[diag] ae2 key max: " + original + " -> " + raised + " (item default " + itemDefault + ")");
            cir.setReturnValue(raised);
        }
    }
}
