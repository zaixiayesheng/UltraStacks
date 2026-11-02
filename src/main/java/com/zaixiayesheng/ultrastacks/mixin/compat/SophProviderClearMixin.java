package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.crafting.SophCraftSlots;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 精妙自己实现的“清空”入口。
 *
 * <p>When Sophisticated is installed its provider declares {@code requiresServerSide()}, so the
 * clear button ends in {@code CraftingUpgradeTweakProvider.clearGrid} rather than the generic
 * {@code DefaultGridClearHandler}. Hooking here keeps the move on the server menu (client and
 * server stay in sync) and lets the menu decide where the stacks land.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks.CraftingUpgradeTweakProvider", remap = false)
public abstract class SophProviderClearMixin {

    @Inject(method = "clearGrid(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;Z)V",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$clearIntoMenu(Player player, AbstractContainerMenu menu, boolean force, CallbackInfo ci) {
        if (menu == null || player == null) {
            return;
        }
        String menuClass = menu.getClass().getName();
        DiagLog.lineThrottled("clearprov." + menuClass, "[diag] clear provider hit: menu=" + menuClass + " force=" + force);
        try {
            var cells = SophCraftSlots.slots(menu);
            boolean intoStorage = SophCraftSlots.intoStorage(menu);
            DiagLog.line("[diag] clear provider toggle intoStorage=" + intoStorage + " cells=" + cells.size());
            int moved = 0;
            for (Slot slot : cells) {
                if (!slot.hasItem()) {
                    continue;
                }
                int before = slot.getItem().getCount();
                if (intoStorage) {
                    menu.quickMoveStack(player, slot.index);
                } else {
                    ultrastacks$toPlayerInventory(menu, slot);
                }
                moved += before - slot.getItem().getCount();
            }
            DiagLog.line("[diag] clear provider moved=" + moved + " cells=" + cells.size() + " force=" + force + " intoStorage=" + intoStorage);
            menu.broadcastChanges();
            ci.cancel();
        } catch (Throwable t) {
            DiagLog.line("[diag] clear provider failed: " + t);
        }
    }

    /** 把一格里的东西并进玩家背包的 36 格，装不下的留在原格。 */
    private static void ultrastacks$toPlayerInventory(AbstractContainerMenu menu, Slot from) {
        net.minecraft.world.item.ItemStack stack = from.getItem();
        for (Slot target : menu.slots) {
            if (stack.isEmpty()) {
                return;
            }
            if (target == from || !(target.container instanceof net.minecraft.world.entity.player.Inventory)) {
                continue;
            }
            if (!target.mayPlace(stack)) {
                continue;
            }
            net.minecraft.world.item.ItemStack there = target.getItem();
            if (!there.isEmpty() && net.minecraft.world.item.ItemStack.isSameItemSameComponents(there, stack)) {
                int room = Math.min(target.getMaxStackSize(stack), stack.getMaxStackSize()) - there.getCount();
                if (room > 0) {
                    int take = Math.min(room, stack.getCount());
                    there.grow(take);
                    stack.shrink(take);
                    target.setChanged();
                }
            }
        }
        for (Slot target : menu.slots) {
            if (stack.isEmpty()) {
                return;
            }
            if (target == from || !(target.container instanceof net.minecraft.world.entity.player.Inventory)) {
                continue;
            }
            if (target.hasItem() || !target.mayPlace(stack)) {
                continue;
            }
            int take = Math.min(target.getMaxStackSize(stack), stack.getMaxStackSize());
            take = Math.min(take, stack.getCount());
            target.set(stack.copyWithCount(take));
            stack.shrink(take);
        }
        from.set(stack);
    }
}
