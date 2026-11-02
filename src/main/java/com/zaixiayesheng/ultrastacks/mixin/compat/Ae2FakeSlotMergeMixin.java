package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 手动往 AE 配置/存储槽里“补货”时，补多少扣多少，剩下的留在手里。
 *
 * <p>原版点击是整摞交换，而 FakeSlot.set 又会把超过容量的部分截断——两者叠加就变成“要么全放、
 * 要么旧存量被抹”。这里在菜单点击入口把“同种物品往已有槽里补”的情况拦下来自己做：算出还能装多少，
 * 写进槽、把手上那摞减掉同样数量，然后直接结束，不进入原版交换逻辑。
 *
 * <p>A merge path for AE fake slots: write what still fits, subtract exactly that from the carried stack,
 * and stop before the vanilla swap (which would either truncate or replace the existing contents).
 */
@Mixin(targets = "appeng.menu.AEBaseMenu", remap = false)
public abstract class Ae2FakeSlotMergeMixin {

    @Inject(method = "clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$mergeIntoFakeSlot(int slotId, int mouseButton, ClickType input, Player player,
        CallbackInfo ci) {
        if (input != ClickType.PICKUP || slotId < 0 || slotId >= ((AbstractContainerMenu) (Object) this).slots.size()) {
            return;
        }
        Slot slot = ((AbstractContainerMenu) (Object) this).slots.get(slotId);
        if (!slot.getClass().getName().startsWith("appeng.menu.slot.FakeSlot")) {
            return;
        }
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        ItemStack carried = menu.getCarried();
        ItemStack existing = slot.getItem();
        // 只在“槽里已有同种物品、手上也有”时接管；空格或异类物品交回原版逻辑。
        if (carried.isEmpty() || existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, carried)) {
            return;
        }
        int cap = Math.min(UltraStacksConfig.maxStackSize(), Math.max(1, slot.getMaxStackSize(existing)));
        int space = Math.max(0, cap - existing.getCount());
        int moved = Math.min(space, carried.getCount());
        if (moved <= 0) {
            // 已经满了：什么都不写，物品留在手里。
            ci.cancel();
            return;
        }
        ItemStack grown = existing.copyWithCount(existing.getCount() + moved);
        slot.set(grown);
        menu.setCarried(carried.copyWithCount(carried.getCount() - moved));
        slot.setChanged();
        menu.broadcastChanges();
        ci.cancel();
    }
}
