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
 * 防止铁砧在输入槽里有多件物品时复制或吞掉一整堆。
 *
 * <p>原版 {@code createResult} 直接 {@code copy()} 左槽整堆作为产物，{@code onTake} 再把左槽清空，于是
 * 附魔与修复变成「一次经验换一整堆」。这里在 {@code createResult} 返回后，把左槽、右槽与产物都非空且左槽
 * 多于一格时的产物压到 1 件；同时重定向 {@code onTake} 里的 {@code Container#setItem}：左槽每次只扣 1
 * 件，右槽按 {@code repairItemCountCost} 扣，没有该代价时扣 1 件。
 *
 * <p>改名不走这条路：右槽为空时保持原版行为，整堆改名、一次取走、只扣一次经验。
 *
 * <p>Keeps the anvil from duplicating or eating a whole stack when the input slot holds more than one
 * item.
 *
 * <p>Vanilla {@code createResult} simply {@code copy()}s the whole left stack into the result slot and
 * {@code onTake} then clears that slot, which turns enchanting and repair into "one payment for a whole
 * stack". After {@code createResult} returns, this clamps the result to a single item whenever the left
 * slot, the additive slot and the result are all non-empty and the left slot holds more than one item.
 * It also redirects the {@code Container#setItem} call inside {@code onTake} so that the left slot loses
 * only one item per operation, while the additive slot loses {@code repairItemCountCost} items, or one
 * when no such cost applies.
 *
 * <p>Renaming does not take that path: with an empty additive slot vanilla behaviour stands — the whole
 * stack is renamed, taken in one go and charged experience once.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

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
            // Pure rename: vanilla clears the whole stack and charges once.
            slots.setItem(index, ItemStack.EMPTY);
            return;
        }
        int amount = index == 1 && self.repairItemCountCost > 0 ? self.repairItemCountCost : 1;
        current.shrink(amount);
        slots.setItem(index, current.isEmpty() ? ItemStack.EMPTY : current);
    }
}
