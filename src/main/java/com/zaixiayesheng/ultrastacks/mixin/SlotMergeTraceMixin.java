package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 诊断 + 修复：原版 {@code AbstractContainerMenu.doClick} 的点击合并链。
 *
 * <p>两件事：</p>
 * <ol>
 *   <li><b>判定取证</b>：redirect 合并判定 {@code isSameItemSameComponents}，把结果
 *       （通过/拒绝）连同两栈数量与组件写进诊断日志。用户报「点一下两堆交换位置」时，
 *       只要这里打印 {@code verdict=false}，就说明组件判定拒绝合并；打印 {@code true}
 *       却仍然不动，则问题在合并量。</li>
 *   <li><b>合并量修复</b>：原版把「一次能合并多少」写死成 {@code 槽内栈的当前数量}
 *       （{@code amount = slotStack.getCount()}）再交给 {@code Slot.safeInsert}。
 *       64 上限时代两堆各 32 恰好够用；上限抬到 65536 后，手里 10944、槽里 7744 时
 *       一次只会并进 7744，看起来就是「堆不起来」。这里把该调用换成
 *       {@code Integer.MAX_VALUE}，让 {@code safeInsert} 一次合并到真正的槽上限。</li>
 * </ol>
 *
 * <p>Two jobs: (1) redirect the merge verdict {@code isSameItemSameComponents} and record
 * whether the two stacks are considered identical, so a rejected merge is visible in the
 * diagnostic log; (2) redirect {@code Slot.safeInsert} so a single click merges the whole
 * stack up to the real slot limit instead of being capped at the slot's current count.</p>
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
    private ItemStack ultrastacks$mergeWholeStack(Slot slot, ItemStack stack, int amount) {
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
