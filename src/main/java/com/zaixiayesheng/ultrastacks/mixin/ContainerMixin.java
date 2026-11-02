package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 容器上限：原版默认 99，这里换成配置值，也不会小于槽里已有的数量。
 * 别的数字是容器自己定的（比如酿造台燃料槽 64），不动。
     *
 * Container limit: vanilla defaults to 99; swap in the configured value and never
 * report less than what the slot already holds. Other numbers were deliberate, leave them.
 */
@Mixin(Container.class)
public interface ContainerMixin {
    @ModifyReturnValue(method = "getMaxStackSize()I", at = @At("RETURN"))
    private int ultrastacks$raiseVanillaLimit(int original) {
        Container self = (Container) this;
        // 零格容器只是占位（神化宝石匣拿 new SimpleContainer(0) 当槽位占位，真正的库存在上限为 1 的
        // 宝石表里）。抬它没有意义，反而让界面以为一格能塞几万个，所以原样返回。
        // A zero-size container is a placeholder — Sophisticated's gem case uses new SimpleContainer(0)
        // as a slot stand-in while the real inventory lives in a gem table with a limit of 1. Raising it
        // makes the UI believe one cell holds tens of thousands, so hand the original back.
        if (self.getContainerSize() == 0) {
            return original;
        }
        return UltraStacksConfig.raiseVanillaDefault(original);
    }

    @ModifyReturnValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private int ultrastacks$neverBelowStackCount(int original, ItemStack stack) {
        return stack.isEmpty() ? original : UltraStacksConfig.honestLimit(original, stack.getCount());
    }
}
