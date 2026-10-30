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
 * AE2 的 {@code AEItemKey.getMaxStackSize()} 决定「从网络里一次拿多少」，不是单纯的容量提示。
 *
 * <p>它原本读的是内部那份只读栈的 {@code getMaxStackSize()}。问题在于本模组让
 * {@code ItemStack.getMaxStackSize()} 至少返回栈里现有的数量——这是为了让 65536 的大堆在槽位里
 * 不显得「超容」。可 AE2 的 key 是从一堆具体物品建出来的，堆里有多少（比如 8479），这个数就跟着
 * 变成 8479，于是终端从网络里一次只搬 8479，看着像「只放/只取一部分」。</p>
 *
 * <p>这里不再采信那个被数量污染的值，改成「物品自己的默认上限」与原本的 64/99 抬升结果取大者：
 * 物品默认上限已经被 {@code ModBusEvents} 按配置烘过（普通物品 65536、鸡蛋 16384），既不会把
 * 本来就只有 1 个上限的工具抬上去，也不再受某堆具体数量的影响。</p>
 *
 * <p>AE2 reads {@code AEItemKey.getMaxStackSize()} as "how many can I move in one go", not as a
 * capacity hint. It forwarded the internal read-only stack's {@code getMaxStackSize()}, and this mod
 * makes that method return at least the stack's own count (so a 65536 stack does not look oversized
 * in a slot). A key built from an 8479 stack therefore reported 8479, and the terminal moved 8479 per
 * operation. Here the value is rebuilt from the item's default limit, which {@code ModBusEvents}
 * already scaled by config, so a stack's count can no longer shrink it.</p>
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
