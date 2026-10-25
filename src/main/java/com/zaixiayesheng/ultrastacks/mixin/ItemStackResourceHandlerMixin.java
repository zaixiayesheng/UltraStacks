package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStackResourceHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 放开 transfer 层单堆处理器的容量上限。
 *
 * <p>{@code ItemStackResourceHandler#getCapacity} 返回 {@code min(resource.getMaxStackSize(), 99)}（资源为
 * 空时直接 99），这个 99 来自常量 {@code Item.ABSOLUTE_MAX_STACK_SIZE}。光标槽的
 * {@code CarriedSlotWrapper} 直接继承这份实现，于是从网络往光标上取物品时一次最多 99 个，堆叠上限配到
 * 65536 也一样：界面格子放得下，光标却卡在 99。
 *
 * <p>这里按 {@code raiseSlotLimit} 的规则把 99 与 64 换成配置上限：只升不降，工具、盔甲这类本来报 1 的
 * 保持原样。
 *
 * <p>Raises the capacity of the transfer layer's single-stack handlers.
 *
 * <p>{@code ItemStackResourceHandler#getCapacity} returns {@code min(resource.getMaxStackSize(), 99)} (a
 * plain 99 for an empty resource), where the 99 comes from the {@code Item.ABSOLUTE_MAX_STACK_SIZE}
 * constant. The cursor slot's {@code CarriedSlotWrapper} inherits that implementation directly, so pulling
 * items from a network onto the cursor stops at 99 even with the stack limit configured to 65536: the grid
 * slot would hold more, the cursor does not.
 *
 * <p>Following the {@code raiseSlotLimit} rule, 99 and 64 are replaced with the configured stack limit;
 * nothing is ever lowered, and values such as the 1 reported for tools and armour are left alone.
 */
@Mixin(ItemStackResourceHandler.class)
public abstract class ItemStackResourceHandlerMixin {

    @ModifyReturnValue(method = "getCapacity", at = @At("RETURN"))
    private int ultrastacks$raiseCapacity(int original, ItemResource resource) {
        return UltraStacksConfig.raiseSlotLimit(original);
    }
}
