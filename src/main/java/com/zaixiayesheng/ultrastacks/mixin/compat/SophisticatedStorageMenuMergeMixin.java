package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 精妙存储、精妙背包的菜单自己有一套合并逻辑。
     *
 * <p>Sophisticated Storage / Backpacks run their own interaction chain and settle merges against
 * {@code slot.getMaxStackSize(stack)}. Fixing vanilla does nothing here; the two paths never meet.</p>
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class SophisticatedStorageMenuMergeMixin {

    private static final Set<String> MERGE_LIMIT_LOGGED = ConcurrentHashMap.newKeySet();
    private static final int MERGE_LIMIT_LOG_CAP = 64;

    /**
     * 把它问的上限抬到模组上限。真抬上了就记一笔，好确认这条链到底有没有被走到。
     *
     * <p>Raises the queried slot limit, and logs when the value actually changes so we can tell
     * whether this path is reached at all.</p>
     */
    @Redirect(
            method = "mergeItemStack(Lnet/minecraft/world/item/ItemStack;IIZZZ)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I"),
            require = 0
    )
    private int ultrastacks$raiseSophMergeLimit(Slot slot, ItemStack stack) {
        int original = slot.getMaxStackSize(stack);
        int raised = Math.max(original, UltraStacksConfig.maxStackSize());
        if (raised != original && MERGE_LIMIT_LOGGED.size() < MERGE_LIMIT_LOG_CAP
                && MERGE_LIMIT_LOGGED.add(stack.getItem() + "@" + slot.getClass().getSimpleName())) {
            UltraStacks.LOGGER.warn("[diag] soph merge slot limit raised: {} x{} limit {} -> {}",
                    stack.getDescriptionId(), stack.getCount(), original, raised);
            DiagLog.line("[diag] soph merge slot limit raised: " + stack.getDescriptionId()
                    + " x" + stack.getCount() + " limit " + original + " -> " + raised);
        }
        return raised;
    }
}
