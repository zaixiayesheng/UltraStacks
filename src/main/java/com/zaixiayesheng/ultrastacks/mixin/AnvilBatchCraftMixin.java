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
            for (int guard = 0; guard < batchCap; guard++) {
                ItemStack before = result.getItem();
                if (before.isEmpty()) {
                    break;
                }
                int beforeCount = before.getCount();
                int leftBefore = menu.slots.get(0).getItem().getCount();
                menu.quickMoveStack(player, index);
                ItemStack after = result.getItem();
                int leftAfter = menu.slots.get(0).getItem().getCount();
                // 取走一次必然消耗左侧一格。只看产物是否相同会误判「重命名」：改名后产物与上一次
                // 完全一样，于是被当成搬不动而提前退出。
                // Taking one always consumes one from the left slot. Comparing only the result would
                // mistake a rename for "nothing moved": the renamed output is identical every round.
                if (leftAfter >= leftBefore && after.getCount() == beforeCount
                        && ItemStack.isSameItemSameComponents(before, after)) {
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
