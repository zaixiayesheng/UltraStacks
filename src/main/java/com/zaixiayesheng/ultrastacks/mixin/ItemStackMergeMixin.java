package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 两堆看着一样的东西，应该能合。
 * 老抽屉模板带了个 max_stack_size=64 的标记，原版一比组件就说不一样，于是永远合不上；
 * 这里把 MAX_STACK_SIZE 摘掉再比，附魔、耐久这些照旧算数。
 * 后来又发现光比补丁还不够：补丁都是空的时候，内容一样但对象不是同一个也会判不等，所以干脆按补丁比。
     *
 * Two identical-looking stacks should merge. Old drawer templates carried a max_stack_size=64
 * component, so vanilla kept saying "different". Compare with MAX_STACK_SIZE ignored, and go by the
 * patch alone — the prototype objects can differ even when their contents match.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMergeMixin {

    /**
     * 判「这两堆算不算同一种东西」的时候先自己拍板，拍不了才交回原版。
     * 三种情况：补丁一模一样 → 同；只差 {@code max_stack_size} 那个组件 → 同（旧的脏栈就走这条）；
     * 其余（附魔、改名、自定义数据不一样）→ 原版说了算。
     *
     * <p>Decides "are these the same thing" first and only falls back to vanilla when it cannot.
     * Equal patches → same; patches differing only in {@code max_stack_size} → same (that is the
     * legacy dirty stack); anything else (enchantments, names, custom data) → vanilla decides.</p>
     */
    @Inject(method = "isSameItemSameComponents", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$mergeDirty(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        if (a == b || a.isEmpty() || b.isEmpty() || !ItemStack.isSameItem(a, b)) {
            return;
        }
        DataComponentPatch pa = a.getComponentsPatch();
        DataComponentPatch pb = b.getComponentsPatch();
        if (pa.equals(pb)) {
            cir.setReturnValue(true);
            return;
        }
        DataComponentPatch strippedA = pa.forget(type -> type == DataComponents.MAX_STACK_SIZE);
        DataComponentPatch strippedB = pb.forget(type -> type == DataComponents.MAX_STACK_SIZE);
        if (strippedA.equals(strippedB)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 让哈希与上面的放宽版 equals 自洽：哈希时忘掉 {@code max_stack_size}。
     *
     * <p>Makes hashing consistent with the relaxed equals above by forgetting
     * {@code max_stack_size} while hashing.
     */
    @Inject(method = "hashItemAndComponents", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$hashForgetMax(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack == null) {
            cir.setReturnValue(0);
            return;
        }
        DataComponentPatch stripped = stack.getComponentsPatch()
                .forget(type -> type == DataComponents.MAX_STACK_SIZE);
        int i = 31 + stack.getItem().hashCode();
        cir.setReturnValue(31 * i + stripped.hashCode());
    }
}
