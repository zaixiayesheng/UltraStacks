package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 精妙交互的整理功能（{@code sortRegion} / {@code insertIntoSlots} / {@code moveStackToTargets}）切堆时
 * 有两个上限来源，都被改成按本模组配置走：
 *
 * <ol>
 *   <li>槽位侧 {@code Slot.getMaxStackSize(物品)}：玩家物品栏背后的 {@code Container} 默认只报 99，65536
 *   的合法栈会被按 1 或 99 切开——改成「至少按配置上限」；</li>
 *   <li>物品侧 {@code ItemStack.getMaxStackSize()}：排序用的 key 栈是数量为 1 的拷贝，而配置助手只有在
 *   {@code 数量 > 上限} 时才抬上限（防止把现存小堆的体积虚报），于是这里永远报 64，大堆被按 64 切。物品栏
 *   只有 36 格装不下，整理直接失败回滚；容器格子多，就拆成一堆 64——改成「按物品原版默认值换算配置上限」，
 *   不再看数量。</li>
 * </ol>
 *
 * <p>Both stack-splitting limits used by Sophisticated Inventory Interactions
 * ({@code sortRegion} / {@code insertIntoSlots} / {@code moveStackToTargets}) are redirected to follow this
 * mod's configuration:
 *
 * <ol>
 *   <li>the slot side {@code Slot.getMaxStackSize(stack)}: the {@code Container} behind the player inventory
 *   still reports 99, so a legal 65536 stack would be chopped into 1- or 99-sized pieces — it now returns at
 *   least the configured limit;</li>
 *   <li>the item side {@code ItemStack.getMaxStackSize()}: the sorting key stack is a count-1 copy, and the
 *   config helper only raises the limit when {@code count > limit} (so existing small stacks are not
 *   over-reported), so this call always reported 64 here and big stacks were chopped into 64s. The player's
 *   36 slots cannot hold them, so the sort failed and rolled back; containers with more slots just got
 *   stacks split into 64s. It now scales the item's vanilla default without looking at the count.</li>
 * </ol>
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.ContainerActionExecutor", remap = false)
public abstract class SophisticatedInventoryInteractionsSortMixin {

    // ---- 槽位侧：Slot.getMaxStackSize(物品) 报容器默认值，抬到配置上限 ----

    @Redirect(
            method = "insertIntoSlots",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I"),
            require = 0)
    private int ultrastacks$raiseSortSlotLimit(Slot slot, ItemStack stack) {
        return Math.max(slot.getMaxStackSize(stack), UltraStacksConfig.maxStackSize());
    }

    /**
     * {@code sortRegion} 在 0.1.15.x 里自带合并/填空两个循环，同样用槽位上限切堆；0.1.14 把循环拆进
     * {@code moveStackToTargets}，这里空转即可（require = 0）。
     */
    @Redirect(
            method = "sortRegion",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I"),
            require = 0)
    private int ultrastacks$raiseSortSlotLimit2(Slot slot, ItemStack stack) {
        return Math.max(slot.getMaxStackSize(stack), UltraStacksConfig.maxStackSize());
    }

    /**
     * 0.1.14 的 {@code moveStackToTargets} 两个循环（并入已有槽、填入空槽）都用槽位上限切堆。
     */
    @Redirect(
            method = "moveStackToTargets",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I"),
            require = 0)
    private int ultrastacks$raiseSortSlotLimit3(Slot slot, ItemStack stack) {
        return Math.max(slot.getMaxStackSize(stack), UltraStacksConfig.maxStackSize());
    }

    // ---- 物品侧：数量为 1 的 key 栈永远报原版默认值，改成按配置换算 ----

    @Redirect(
            method = "insertIntoSlots",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"),
            require = 0)
    private int ultrastacks$raiseSortItemLimit(ItemStack stack) {
        return UltraStacksConfig.sortCapacity(stack);
    }

    @Redirect(
            method = "sortRegion",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"),
            require = 0)
    private int ultrastacks$raiseSortItemLimit2(ItemStack stack) {
        return UltraStacksConfig.sortCapacity(stack);
    }

    @Redirect(
            method = "moveStackToTargets",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"),
            require = 0)
    private int ultrastacks$raiseSortItemLimit3(ItemStack stack) {
        return UltraStacksConfig.sortCapacity(stack);
    }
}
