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
 * 铁砧不能把一整堆当成「一次附魔」或「一次修复」的代价。
 *
 * <p>原版会把左槽整堆复制进产物槽，取走产物时再清空整槽。堆叠上限抬起来之后，一次附魔或一次修复
 * 就变成拿一整堆去换一件东西，所以这类产物在这里压回 1 件，输入也按件扣除。
 *
 * <p>改名保持原版手感：整堆一起改名、一次取走、只扣一次经验——那正是玩家想要的批量操作。
 *
 * <p>Keeps the anvil from treating a whole stack as the price of one enchantment or one repair.
 * Vanilla copies the entire left stack into the result slot and clears that slot when the result is
 * taken; with a raised limit one operation would consume a stack for a single item, so those results
 * are clamped back to one and the input is charged per item. Renaming still works on the whole stack
 * at once and charges its experience once, which is the bulk operation players expect.
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
            // 纯改名：原版把整堆清空并只扣一次经验，保持这种批量行为。
            slots.setItem(index, ItemStack.EMPTY);
            return;
        }
        int amount = index == 1 && self.repairItemCountCost > 0 ? self.repairItemCountCost : 1;
        current.shrink(amount);
        slots.setItem(index, current.isEmpty() ? ItemStack.EMPTY : current);
    }
}
