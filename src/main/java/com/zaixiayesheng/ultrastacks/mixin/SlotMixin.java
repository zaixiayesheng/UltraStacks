package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 槽位上限兜底：99 换成配置值；同一种物品时不低于槽里已有的数量，
 * 不然 safeInsert 会按「上限减数量」算出的负数去拆，越拆越乱。
     *
 * Slot limit: swap the 99, and for the same item never go below the current
 * count — otherwise safeInsert splits by a negative number.
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
