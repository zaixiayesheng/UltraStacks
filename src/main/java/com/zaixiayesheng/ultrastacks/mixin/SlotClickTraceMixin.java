package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import java.util.List;
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
 * 点击入口的记录。当初玩家说合不上，可挂在比较上的记录一条都没有，说明点击压根没走到那儿，
 * 探针就挪到了这里：槽位、点击类型、两堆的数量和补丁、能不能放、判不判同。
 * 有记录就能顺着找是谁拒绝的；没有，那就是被别的模组接走了。
     *
 * What really happens at the click entry point. The comparison probe stayed silent while merges kept
 * failing, so the probe moved here: slot, click type, both stacks, mayPlace and the equality verdict.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class SlotClickTraceMixin {

    private static final java.util.Set<String> CLICK_TRACE_SEEN =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private static final int CLICK_TRACE_CAP = 200;

    @Inject(method = "clicked(IILnet/minecraft/world/inventory/ClickType;"
            + "Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), require = 0)
    private void ultrastacks$traceSlotClick(int slotId, int button, ClickType clickType,
            Player player, CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        List<Slot> slots = self.slots;
        if (slotId < 0 || slotId >= slots.size()) {
            return;
        }
        Slot slot = slots.get(slotId);
        ItemStack carried = self.getCarried();
        ItemStack slotStack = slot.getItem();
        // 只关心双方都有物品的点击（合并/交换场景）。
        // Only clicks with items on both sides (merge/swap scenario) matter.
        if (carried.isEmpty() || slotStack.isEmpty()) {
            return;
        }
        if (CLICK_TRACE_SEEN.size() >= CLICK_TRACE_CAP) {
            return;
        }
        String key = clickType + "|" + slotId + "|" + carried.getDescriptionId() + "|"
                + carried.getCount() + "|" + carried.getComponentsPatch() + "|"
                + slotStack.getDescriptionId() + "|" + slotStack.getCount() + "|"
                + slotStack.getComponentsPatch();
        if (!CLICK_TRACE_SEEN.add(key)) {
            return;
        }
        boolean mayPlace = slot.mayPlace(carried);
        boolean same = ItemStack.isSameItemSameComponents(carried, slotStack);
        int carriedMax = carried.getMaxStackSize();
        int slotMax = slot.getMaxStackSize(carried);
        String line = "[diag] slot click: id=" + slotId + " type=" + clickType
                + " carried=" + carried.getDescriptionId() + " x" + carried.getCount()
                + " patch=" + carried.getComponentsPatch()
                + " slot=" + slotStack.getDescriptionId() + " x" + slotStack.getCount()
                + " patch=" + slotStack.getComponentsPatch()
                + " mayPlace=" + mayPlace + " same=" + same
                + " carriedMax=" + carriedMax + " slotMax=" + slotMax;
        UltraStacks.LOGGER.warn(line);
        com.zaixiayesheng.ultrastacks.DiagLog.line(line);
    }
}
