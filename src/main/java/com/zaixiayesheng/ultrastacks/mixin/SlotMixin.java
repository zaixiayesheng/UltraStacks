package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 槽位上限的两层兜底。
 *
 * <p>第一层：原版 {@code Slot#getMaxStackSize()} 直接转给容器的上限，而容器默认是 99，这里把 99 换成
 * 配置值，并且返回值不小于槽内已有的数量。
 *
 * <p>第二层：{@code getMaxStackSize(ItemStack)} 原版取「槽位上限」与「物品上限」的较小值。上限若小于
 * 槽内已有数量，{@code Slot#safeInsert} 里的 {@code 上限 - 数量} 会变成负数，随后拿它去 split / shrink
 * 就会一边变少一边变多。所以当槽里是同一种物品时，返回值至少是当前数量；不同物品不受影响——槽里堆着
 * 一千个铁锭，不该让十六个一组的鸡蛋也报出同样的上限。
 *
 * <p>Two layers of slot-limit protection. First, vanilla {@code Slot#getMaxStackSize()} forwards to the
 * container limit, which defaults to 99; 99 is replaced with the configured value and the result is
 * never below the stack already in the slot. Second, {@code getMaxStackSize(ItemStack)} takes the
 * smaller of the slot limit and the item limit in vanilla. If the limit is below the count already
 * in the slot, {@code Slot#safeInsert} computes {@code limit - count} as a negative number and then
 * splits or shrinks by it, losing on one side while gaining on the other. For the same item the
 * result is therefore at least the current count; other items are unaffected, because a slot holding
 * a thousand iron ingots must not report that limit for a stack of sixteen eggs.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @ModifyReturnValue(method = "getMaxStackSize()I", at = @At("RETURN"))
    private int ultrastacks$raiseVanillaLimit(int original) {
        int raised = UltraStacksConfig.raiseVanillaDefault(original);
        ItemStack current = ((Slot) (Object) this).getItem();
        return current.isEmpty() ? raised : Math.max(raised, current.getCount());
    }

    @ModifyReturnValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private int ultrastacks$neverBelowContents(int original, ItemStack stack) {
        // 有些槽位（例如酿造台药水槽）自己重写了无参版本并固定返回 1，上层注入改不到它；这里至少保证同物品时不低于槽内数量。
        int raised = UltraStacksConfig.raiseSlotLimit(original);
        ItemStack current = ((Slot) (Object) this).getItem();
        // 只对同一种物品兜底：槽里堆着一千个铁锭时，不能因此让十六个一组的鸡蛋也塞到一千。
        if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, stack)) {
            return raised;
        }
        return Math.max(raised, current.getCount());
    }
}
