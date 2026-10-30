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
 * 诊断：在 {@code AbstractContainerMenu.slotClicked} 入口记录每次「合并/交换」点击的完整现场。
 *
 * <p>背景：用户复现「两堆泥土不能合并、点击后交换位置」，但 {@code isSameItemSameComponents}
 * 上的无差别 trace 零记录——说明该次点击从未走到原版合并判定。本 mixin 在 slotClicked 入口
 * 记录槽位 id、点击类型、手拿栈与目标槽栈的数量/组件、mayPlace 与组件判等结果，用以确证：
 * (1) 点击是否真的到达原版 slotClicked；(2) 若到达，判定链上哪一环拒绝合并。
 * 只记双方都有物品的点击（合并/交换场景），去重 + 限量，写 LOGGER 与 DiagLog。</p>
 *
 * <p>Diagnostic: at the head of {@code AbstractContainerMenu.slotClicked}, snapshot every
 * merge/swap click — slot id, click type, carried and slot stacks with counts and patches, the
 * mayPlace verdict and the component-equality verdict. Used to prove whether the click reaches
 * vanilla slotClicked at all, and if it does, which check refuses the merge. Only clicks where
 * both sides hold items are recorded; deduplicated and capped, written to the LOGGER and
 * DiagLog.</p>
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
