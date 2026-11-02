package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.crafting.AnvilBatchGuard;
import com.zaixiayesheng.ultrastacks.crafting.UnlockState;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 铁砧支持 shift 连续合成：一次 shift 把这一摞按「一次一份」全部做完。
 *
 * <p>Lets shift-click finish a whole stack on the anvil. Vanilla moves one result per shift-click;
 * this re-enters vanilla's own move logic until the output slot stops changing, so each round still
 * charges the normal price, consumes one item from each input slot and plays the anvil sound.
 */
@Mixin(ItemCombinerMenu.class)
public abstract class AnvilBatchCraftMixin {

    /** 防止递归重入：内层调用必须走原版逻辑。 / Guards re-entry so the inner call runs vanilla. */
    private static final ThreadLocal<Boolean> ULTRASTACKS_IN_BATCH = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$batchTake(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!ULTRASTACKS_IN_BATCH.get()) {
            AbstractContainerMenu m = (AbstractContainerMenu) (Object) this;
            Slot clicked = index >= 0 && index < m.slots.size() ? m.slots.get(index) : null;
            com.zaixiayesheng.ultrastacks.DiagLog.lineThrottled("anvil.click." + index,
                "[diag] anvil click: menu=" + this.getClass().getName() + " index=" + index
                    + " resultSlot=" + ((ItemCombinerMenu) (Object) this).getResultSlot()
                    + " clicked=" + (clicked == null ? "null" : clicked.getClass().getSimpleName())
                    + " anvilMenu=" + ((Object) this instanceof AnvilMenu));
        }
        if (ULTRASTACKS_IN_BATCH.get() || !((Object) this instanceof AnvilMenu)) {
            return;
        }
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (index != 2) {
            return;
        }
        Slot result = menu.slots.get(2);
        if (result.getItem().isEmpty()) {
            return;
        }
        // 没按解锁键就只做 bulkCraftLimit 次；按住解锁不设上限，做空左槽为止。循环本身靠「左槽
        // 是否被消耗」退出，所以不需要额外保险丝。
        // Without the unlock key only bulkCraftLimit crafts happen; holding it runs until the left
        // slot is empty. The loop exits on its own because it watches that slot being consumed.
        boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey() && UnlockState.isUnlocked(player);
        int batchCap = unlocked ? Integer.MAX_VALUE : Math.max(1, UltraStacksConfig.bulkCraftLimit());
        ULTRASTACKS_IN_BATCH.set(Boolean.TRUE);
        AnvilBatchGuard.enterLoop();
        try {
            // 精妙的铁砧是外层菜单里的子菜单，产物槽不在玩家菜单里；在它上面做批量会把子菜单的
            // 状态搞乱（实测点一次之后再也合成不出来），所以精妙这条一律交还原版一次一件。
            // Sophisticated's anvil lives in a sub-menu; batching there corrupts its state, so leave
            // Sophisticated to vanilla one-at-a-time and only batch the plain anvil menu.
            if (this.getClass().getName().startsWith("net.p3pp3rf1y.")) {
                return;
            }
            for (int guard = 0; guard < batchCap; guard++) {
                if (result.getItem().isEmpty()) {
                    break;
                }
                ItemStack taken = menu.quickMoveStack(player, index);
                if (taken.isEmpty()) {
                    break;
                }
            }
        } finally {
            AnvilBatchGuard.leaveLoop();
            ULTRASTACKS_IN_BATCH.set(Boolean.FALSE);
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }
}
