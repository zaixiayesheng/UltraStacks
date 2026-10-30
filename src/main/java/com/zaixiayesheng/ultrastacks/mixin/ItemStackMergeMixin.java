package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让「脏物品」能和干净物品合并，并让「补丁相同但默认组件表对象不同」的两堆也能合并。
 *
 * <p>原版 {@code ItemStack.isSameItemSameComponents} 先比物品、再比 {@code PatchedDataComponentMap}。
 * 那个 {@code equals} 除了比补丁，还会比 prototype——也就是物品默认组件表。存档读回来、复制、其他模组
 * 重设默认表，都会让两堆「内容完全一样」的东西拿着不同的 prototype 实例，于是被判成不同物品：点一下
 * 只交换不合并，重进世界所有栈从存档重建、prototype 统一又恢复正常。
 * 这里在入口处接管：补丁相同即判同；补丁只差 {@code max_stack_size} 也判同；其余情况（附魔、名字、
 * 自定义数据不同）仍交给原版。
 *
 * <p>Lets "dirty" stacks merge with clean ones, and lets two stacks with equal patches merge even when
 * their prototypes (item default component maps) are different instances. Vanilla
 * {@code PatchedDataComponentMap.equals} compares the prototype too, so two stacks with identical
 * contents can be treated as different items — a click swaps them instead of merging, and only a world
 * reload (which rebuilds every stack from the save and unifies the prototype) fixes it. This entry point
 * accepts equal patches and patches differing only in {@code max_stack_size}; everything else still goes
 * through vanilla.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMergeMixin {

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
