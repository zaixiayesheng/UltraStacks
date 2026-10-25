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
 * 铁砧：输入格超过一件时，既不整摞复制产物，也不吞掉整摞。
 *
 * <p>原版把左槽整摞复制成产物，取走产物时又清空左槽；对附魔和修复来说，这等于一次经验换一整摞。
 * 所以这两类产物被压到 1 件，输入也改成按件扣。改名保持原版行为：整摞一起改名、一次取走、
 * 只扣一次经验。
 *
 * <p>Keeps the anvil from duplicating or eating a whole stack once an input slot holds more than
 * one item. Vanilla copies the entire left stack into the result slot and clears that slot when
 * the result is taken; for enchanting and repair that turns one payment into a whole stack.
 * Those results are therefore clamped to a single item and the input is consumed per item.
 * Renaming keeps the vanilla behaviour: the whole stack is renamed, taken in one go and charged
 * experience once.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    /**
     * 产物出现后压到 1 件，取走时才不会被当成整摞一次操作。
     *
     * <p>只有左槽超过一件、且右槽与产物都有物品（附魔或修复）时才压。纯改名没有右槽物品，整摞保留。
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
     * <p>左槽纯改名时照原版整个清空；右槽作为修复材料时按 {@code repairItemCountCost} 扣，其余情况
     * 一次只扣 1 件。这样取走产物只为这一次操作结算材料。
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
