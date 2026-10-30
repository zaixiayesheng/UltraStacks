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
 * 精妙存储/精妙背包的容器菜单（sophcore 的 StorageContainerMenuBase）重写了
 * 点击/快速转移/整理的全部交互逻辑，合并容量走的是它自己的
 * {@code mergeItemStack(ItemStack, int, int, boolean, boolean, boolean)}，
 * 上限取自 {@code slot.getMaxStackSize(stack)}（即 {@code handler.getStackLimit(slot, stack)}）。
 *
 * 这条链与原版 slotClicked 完全分开，原版诊断对它不可见。这里把它的槽位
 * 上限也抬到模组 maxStackSize，避免精妙箱子/背包内的合并被自身上限卡住。
 *
 * <b>中文</b>：精妙菜单的合并容量在这里统一抬高到模组上限。
 * <b>EN</b>: Raise the per-slot merge capacity inside Sophisticated Storage/Backpacks
 * menus, which use their own mergeItemStack path invisible to vanilla diagnostics.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class SophisticatedStorageMenuMergeMixin {

    private static final Set<String> MERGE_LIMIT_LOGGED = ConcurrentHashMap.newKeySet();
    private static final int MERGE_LIMIT_LOG_CAP = 64;

    /**
     * <b>中文</b>：把精妙菜单合并循环里的槽位上限抬高到模组上限。
     * <b>EN</b>: Raise the slot limit used by the Sophisticated menu merge loop.
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
