package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 容器上限：把原版默认的 99 换成配置值，并保证返回值不小于槽内那一摞的数量。
 *
 * <p>{@link Container#getMaxStackSize()} 在原版里默认返回 99，没重写它的容器（箱子、潜影盒等）
 * 都吃这个值，于是物品能堆多高都只能放到 99；{@code getMaxStackSize(ItemStack)} 则是
 * 「容器上限」与「物品上限」取小。第一个钩子只替换 99，返回其他数字的容器是刻意指定了上限（例如
 * 酿造台燃料槽的 64），保持原样。
 *
 * <p>第二个钩子不能只做替换：{@code SimpleContainer#setItem} 会用
 *  {@code stack.limitSize(getMaxStackSize(stack))} 截断传进来的堆叠，上限若小于堆叠数量，整摞会被砍掉一截。
 * 所以对非空堆叠返回 {@code max(原值, 数量)}。
 *
 * <p>Replaces the vanilla default of 99 with the configured limit and never reports less than the
 * stack already in the slot. {@code Container#getMaxStackSize()} returns 99 by default, so every
 * container that does not override it, such as a chest or a shulker box, caps any item at 99 no
 * matter how high it stacks; {@code getMaxStackSize(ItemStack)} takes the smaller of the container
 * limit and the item limit. The first hook substitutes only 99: a container returning another
 * number chose that limit deliberately (the brewing stand fuel slot returns 64) and is left alone.
 * The second hook cannot merely substitute the limit, because {@code SimpleContainer#setItem}
 * truncates an incoming stack with {@code stack.limitSize(getMaxStackSize(stack))}, so a limit
 * below the stack size would cut the stack down; for a non-empty stack the result is therefore
 * {@code max(original, count)}.
 */
@Mixin(Container.class)
public interface ContainerMixin {
    @ModifyReturnValue(method = "getMaxStackSize()I", at = @At("RETURN"))
    private int ultrastacks$raiseVanillaLimit(int original) {
        return UltraStacksConfig.raiseVanillaDefault(original);
    }

    @ModifyReturnValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private int ultrastacks$neverBelowStackCount(int original, ItemStack stack) {
        return stack.isEmpty() ? original : Math.max(original, stack.getCount());
    }
}
