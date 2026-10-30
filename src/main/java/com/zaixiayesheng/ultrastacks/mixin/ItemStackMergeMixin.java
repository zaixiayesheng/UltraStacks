package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让两堆看着一样的东西真能合到一起。
 *
 * <p>麻烦出在 {@code PatchedDataComponentMap.equals}：它除了比补丁，还比 prototype——物品那张默认组件表。
 * 存档读回来、复制、别的模组重设默认表，都会让内容一模一样的两堆拿着不同的 prototype 实例，于是被判成
 * 不同东西，点一下只交换不合。重进世界全部重建、prototype 统一，就又好了。这里在入口接管：补丁一样就判同，
 * 只差 {@code max_stack_size} 也判同，剩下的（附魔、名字、自定义数据）照旧交给原版。</p>
 *
 * <p>{@code PatchedDataComponentMap.equals} compares the prototype — the item's default component map —
 * on top of the patch, so two identical stacks can be judged different just because one came back from
 * a save or was copied. Equal patches are accepted here, and so are patches differing only in
 * {@code max_stack_size}; everything else falls through to vanilla.</p>
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
