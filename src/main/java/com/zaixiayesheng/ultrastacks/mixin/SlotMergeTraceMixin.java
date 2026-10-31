package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 点击一次能并进去多少。
 * 原版拿“槽里现在有多少”当合并上限，64 时代够用，抬到 65536 就只并那么一点点。
 * 现在左键点在已经有东西的格子上，按整堆并；诊断只在判定拒绝的时候打一行。
     *
 * How much a single click merges. Vanilla caps it at the slot's current count — fine at 64, useless
 * at 65536. Left-clicking a slot that already holds the item now merges the whole stack.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class SlotMergeTraceMixin {

    private static final java.util.Set<String> VERDICT_SEEN =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private static final int VERDICT_CAP = 200;

    @Redirect(
            method = "doClick(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 0)
    private static boolean ultrastacks$traceMergeCompare(ItemStack a, ItemStack b) {
        boolean same = ItemStack.isSameItemSameComponents(a, b);
        if (a.getCount() > 1 || b.getCount() > 1) {
            if (VERDICT_SEEN.size() < VERDICT_CAP) {
                String key = a.getDescriptionId() + '|' + a.getCount() + '|'
                        + a.getComponentsPatch() + '|' + b.getCount() + '|'
                        + b.getComponentsPatch() + '|' + same;
                if (VERDICT_SEEN.add(key)) {
                    String line = "[diag] merge verdict=" + same + ": " + a.getDescriptionId()
                            + " x" + a.getCount() + " patchA=" + a.getComponentsPatch()
                            + " vs x" + b.getCount() + " patchB=" + b.getComponentsPatch()
                            + " maxA=" + a.getMaxStackSize() + " maxB=" + b.getMaxStackSize();
                    UltraStacks.LOGGER.warn(line);
                    com.zaixiayesheng.ultrastacks.DiagLog.line(line);
                }
            }
        }
        return same;
    }

    /**
     * 注意：{@code Slot.safeInsert} 是<b>实例方法</b>，Mixin 要求重定向它的处理器也必须是
     * <b>实例方法</b>（第一个参数接住接收者 {@code Slot}）。写成 {@code static} 会静默匹配失败，
     * 这正是上一版「改了却完全没生效」的原因。
     *
     * <p>Note: {@code Slot.safeInsert} is an instance method, so its redirect handler must be an
     * instance method too (the first parameter receives the {@code Slot} receiver). Declaring it
     * {@code static} silently fails to apply — which is why the previous build changed nothing.</p>
     */
    @Redirect(
            method = "doClick(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;"),
            require = 0)
    private ItemStack ultrastacks$mergeWholeStack(Slot slot, ItemStack stack, int amount,
                                                  @Local(argsOnly = true, index = 1) int button) {
        // 右键（button 1）一律交还原版：那里 amount 就是 1，把「放一个」变成「放一整摞」。
        if (button != 0 || !slot.hasItem()) {
            return slot.safeInsert(stack, amount);
        }
        if (amount != Integer.MAX_VALUE && (stack.getCount() > 1 || slot.getItem().getCount() > 1)) {
            String line = "[diag] merge raise: " + stack.getDescriptionId() + " carried x"
                    + stack.getCount() + " into slot x" + slot.getItem().getCount()
                    + " vanillaAmount=" + amount + " -> MAX";
            UltraStacks.LOGGER.warn(line);
            com.zaixiayesheng.ultrastacks.DiagLog.line(line);
        }
        return slot.safeInsert(stack, Integer.MAX_VALUE);
    }
}
