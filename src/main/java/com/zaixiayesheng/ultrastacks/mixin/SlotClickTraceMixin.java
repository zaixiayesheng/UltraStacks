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
 * 诊断用：在 {@code AbstractContainerMenu.clicked} 入口把每次「合并/交换」点击的现场记下来。
 *
 * <p>当时是真卡住了。玩家说两堆泥土一点只是换来换去、压根不合，可挂在
 * {@code isSameItemSameComponents} 上的记录一条都没有——说明那一下点击压根没走到原版判等。
 * 那就把探针往前挪，挪到点击入口本身：槽位 id、点击类型、手里和槽里各自的数量与组件、
 * {@code mayPlace} 的结果、还有判等的结果。这两种情况都好办：有记录，顺着往下看是哪一环拒的；
 * 没记录，那就是点击被别的模组整个接走了。</p>
 *
 * <p>只记双方都有东西的点击（合并、交换才会发生），而且去重加限量——之前被后台扫描的 x1 对 x1
 * 把额度刷光过一次，长记性了。日志同时写 LOGGER 和 DiagLog，不退游戏也能读。</p>
 *
 * <p>Records what actually happened at the click entry point. Back then the trace on
 * {@code isSameItemSameComponents} stayed completely silent while the player kept failing to merge,
 * which meant the click never reached vanilla's comparison at all. So the probe moved here: slot id,
 * click type, both stacks, the {@code mayPlace} verdict and the equality verdict. A record means we
 * can follow it down to the refusing check; no record means something else swallowed the click.</p>
 *
 * <p>Only clicks with items on both sides are recorded, deduplicated and capped — background x1-vs-x1
 * comparisons ate the whole budget once already. Written to both LOGGER and DiagLog.</p>
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
