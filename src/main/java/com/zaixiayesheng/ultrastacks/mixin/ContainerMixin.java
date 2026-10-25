package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 改两项容器上限：原版默认的 99，以及「不得小于槽内数量」这条下界。
 *
 * <p>{@link Container#getMaxStackSize()} 默认返回 99，箱子、潜影盒这类没有重写它的容器都会吃这个值，这
 * 里换成配置的堆叠上限；{@code getMaxStackSize(ItemStack)} 本身是 {@code min(容器上限, 物品上限)}，再
 * 把返回值抬到不小于传入堆叠的数量（堆叠为空时不处理）。
 *
 * <p>第二条不能省：{@code SimpleContainer#setItem} 与 {@code BaseContainerBlockEntity#setItem} 收物品时
 * 会调 {@code stack.limitSize(getMaxStackSize(stack))}，上限若小于数量，堆叠会被直接截断，玩家看到的就是
 * 「放进去少了一大截」。
 *
 * <p>Two container limits are changed: the vanilla default of 99, and the floor that keeps a limit from
 * dropping below the stack already in the slot.
 *
 * <p>{@link Container#getMaxStackSize()} returns 99 by default, and every container that does not
 * override it — chests, shulker boxes — inherits that value, so it is replaced with the configured stack
 * limit. {@code getMaxStackSize(ItemStack)} is itself {@code min(container limit, item limit)}, and its
 * return value is raised to at least the size of the stack that was passed in (an empty stack is left
 * alone).
 *
 * <p>The second part is not optional: {@code SimpleContainer#setItem} and
 * {@code BaseContainerBlockEntity#setItem} call {@code stack.limitSize(getMaxStackSize(stack))} when they
 * take an item, and a limit below the count truncates the stack on the spot, which looks to the player
 * like "most of what I put in just vanished".
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
