package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 精妙交互整理时的两处上限。
 * 槽位那边：玩家背包背后的容器还报 99，65536 的栈会被切成小块，所以至少放到配置值。
 * 物品那边：排序用的 key 是数量 1 的拷贝，配置助手只在“数量 > 上限”时才抬，于是这里老报 64，
 * 大堆被切成 64 一堆——背包 36 格装不下就整体回滚，格子多的容器就被拆散。现在按物品默认值算。
     *
 * The two limits Sophisticated Inventory Interactions uses while sorting: the slot side (the player
 * container still reports 99 and chops a 65536 stack) and the item side (the count-1 key stack always
 * reported 64, splitting big stacks).
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
