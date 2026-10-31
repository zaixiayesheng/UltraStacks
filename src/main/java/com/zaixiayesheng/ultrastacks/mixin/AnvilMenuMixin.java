package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 防止铁砧在左槽有多件时复制或吞掉一整堆：产物压到 1 件，取走时左槽扣 1、右槽按代价扣。
 * 改名不走这条路，整堆改名、只扣一次经验。
     *
 * Keeps the anvil from duplicating or eating a stack: clamp the result to one item
 * and charge one left-slot item per take. Renaming stays vanilla.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    /**
     * 产物出现后压到 1 件，取走时才不会被当成整摞一次操作。
     *
     * <p>Clamps the result to one item so that taking it cannot treat a whole stack as a single
     * operation. Only a stack larger than one with both the additive slot and the result filled
     * (enchanting or repair) is clamped; a pure rename has no additive item and keeps the whole
     * stack.
     */
    @Inject(method = "createResult", at = @At("RETURN"))
    private void ultrastacks$clampResult(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        Slot left = self.getSlot(0);
        Slot additive = self.getSlot(1);
        Slot result = self.getSlot(2);
        if (!additive.hasItem() || !result.hasItem() || !left.hasItem()) {
            return;
        }
        if (left.getItem().getCount() > 1) {
            result.getItem().setCount(1);
        }
    }

    /**
     * 把原版的「清空整槽」换成按件扣输入。
     *
     * <p>Replaces vanilla's "clear the whole slot" with a per-item consumption. A pure rename still
     * clears the left slot exactly as vanilla does; the additive slot is charged by
     * {@code repairItemCountCost} while it serves as repair material and one item otherwise. Taking
     * the result therefore only pays for what this one operation used.
     */
    @Redirect(
        method = "onTake",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V")
    )
    private void ultrastacks$consumeInput(Container slots, int index, ItemStack value) {
        ItemStack current = slots.getItem(index);
        if (current.isEmpty()) {
            slots.setItem(index, value);
            return;
        }
        AnvilMenu self = (AnvilMenu) (Object) this;
        if (index == 0 && !self.getSlot(1).hasItem()) {
            // 纯改名：原版整摞清空、只扣一次经验，这里照原样执行。
            slots.setItem(index, ItemStack.EMPTY);
            return;
        }
        int amount = index == 1 && self.repairItemCountCost > 0 ? self.repairItemCountCost : 1;
        current.shrink(amount);
        slots.setItem(index, current.isEmpty() ? ItemStack.EMPTY : current);
    }
}
