package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 两堆看着一样的东西，应该能合。
 * 老抽屉模板带了个 max_stack_size=64 的标记，原版一比组件就说不一样，于是永远合不上；
 * 这里把 MAX_STACK_SIZE 摘掉再比，附魔、耐久这些照旧算数。
 * 后来又发现光比补丁还不够：补丁都是空的时候，内容一样但对象不是同一个也会判不等，所以干脆按补丁比。
     *
 * Two identical-looking stacks should merge. Old drawer templates carried a max_stack_size=64
 * component, so vanilla kept saying "different". Compare with MAX_STACK_SIZE ignored, and go by the
 * patch alone — the prototype objects can differ even when their contents match.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMergeMixin {

    /**
     * 限流用的去重集合：同物品 + 相同补丁对的「合并被拒」只记录一次。
     * Dedup set for rate limiting: each item + patch pair is logged at most once.
     */
    private static final java.util.Set<String> MERGE_BLOCKED_SEEN =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /** 每个物品最多记录的「合并被拒」行数。 / Max merge-blocked lines per item. */
    private static final java.util.Map<String, Integer> MERGE_BLOCKED_PER_ITEM =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 每个物品最多记录的「合并被拒」行数。 / Max merge-blocked lines per item id. */
    private static final int MERGE_BLOCKED_PER_ITEM_CAP = 25;

    /** 每个游戏会话最多记录的「合并被拒」行数。 / Cap of merge-blocked lines per game session. */
    private static final int MERGE_BLOCKED_CAP = 1000;

    @Inject(method = "isSameItemSameComponents", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$mergeDirty(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        // 引用相同、有空格或物品本身不同：交给原逻辑。
        // Same reference, an empty side, or different items: leave the original logic alone.
        if (a == b || a.isEmpty() || b.isEmpty() || !ItemStack.isSameItem(a, b)) {
            return;
        }
        DataComponentPatch pa = a.getComponentsPatch();
        DataComponentPatch pb = b.getComponentsPatch();
        if (pa.equals(pb)) {
            // 补丁完全相同，但原版仍可能判 false：{@code PatchedDataComponentMap.equals} 还会比较
            // prototype（物品默认组件表）。运行时两栈的默认组件表可能不是同一个对象（复制、
            // 反序列化、其他模组 setAll 都会换掉它），于是「补丁一样、物品一样」的两堆被判成不同，
            // 表现为点了只交换不合并。这里直接以补丁为准。
            //
            // Identical patches can still compare unequal because PatchedDataComponentMap.equals also
            // compares the prototype (the item default component map). At runtime two stacks can hold
            // different prototype instances even though their effective components are the same, which
            // makes vanilla treat them as different. Trust the patch instead.
            boolean sameMap = a.getComponents().equals(b.getComponents());
            if (!sameMap) {
            } else {
            }
            cir.setReturnValue(true);
            return;
        }
        DataComponentPatch strippedA = pa.forget(type -> type == DataComponents.MAX_STACK_SIZE);
        DataComponentPatch strippedB = pb.forget(type -> type == DataComponents.MAX_STACK_SIZE);
        if (strippedA.equals(strippedB)) {
            // 唯一差别就是 max_stack_size 组件：视为相同物品。
            // The only difference is the max_stack_size component: treat them as the same item.
            cir.setReturnValue(true);
        } else {
            trace("blocked", a, b, pa, pb);
            // 诊断：同物品但组件不同，合并被拒。打印双方完整补丁，用于定位隐形组件。
            // 只记录「真合并尝试」：至少一方数量 > 1。后台扫描（EMI/排序等）都是 x1 对 x1 的
            // 两两比较，会把限流额度刷光、淹没玩家手动合并的记录。
            // Diagnostic: same item but components differ and the merge is refused. Dump both
            // patches to identify the invisible component. Only log real merge attempts (at
            // least one side has count > 1); background scans compare x1-vs-x1 pairs and would
            // flood the cap.
            if (a.getCount() > 1 || b.getCount() > 1) {
                String itemKey = a.getDescriptionId();
                Integer logged = MERGE_BLOCKED_PER_ITEM.get(itemKey);
                if ((logged == null || logged < MERGE_BLOCKED_PER_ITEM_CAP)
                        && MERGE_BLOCKED_SEEN.size() < MERGE_BLOCKED_CAP) {
                    String key = itemKey + '|' + pa + '|' + pb;
                    if (MERGE_BLOCKED_SEEN.add(key)) {
                        MERGE_BLOCKED_PER_ITEM.put(itemKey, (logged == null ? 0 : logged) + 1);
                        UltraStacks.LOGGER.warn(
                                "[diag] merge blocked: {} x{} patch={}  VS  {} x{} patch={}",
                                itemKey, a.getCount(), pa,
                                b.getDescriptionId(), b.getCount(), pb);
                        com.zaixiayesheng.ultrastacks.DiagLog.line(
                                "[diag] merge blocked: " + itemKey + " x" + a.getCount()
                                        + " patch=" + pa + "  VS  " + b.getDescriptionId()
                                        + " x" + b.getCount() + " patch=" + pb);
                    }
                }
            }
        }
    }

    /** 无差别合并比较记录的去重键集合。 / Dedup set for the unconditional merge-compare trace. */
    private static final java.util.Set<String> TRACE_SEEN =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /** 无差别 trace 的会话总量上限。 / Per-session cap of unconditional trace lines. */
    private static final int TRACE_CAP = 400;

    /**
     * 每次「同物品」比较都记一条 trace（去重 + 上限），用于确证合并判定是否真的经过
     * {@code isSameItemSameComponents}。玩家手动合并的两堆数量较大，后台扫描多为 x1 对 x1，
     * 因此只记至少一方数量 > 1 的比较。
     */
    private static void trace(String verdict, ItemStack a, ItemStack b,
                              DataComponentPatch pa, DataComponentPatch pb) {
        if (a.getCount() <= 1 && b.getCount() <= 1) {
            return;
        }
        if (TRACE_SEEN.size() >= TRACE_CAP) {
            return;
        }
        String key = verdict + '|' + a.getDescriptionId() + '|' + a.getCount() + '|'
                + b.getCount() + '|' + pa + '|' + pb;
        if (TRACE_SEEN.add(key)) {
            UltraStacks.LOGGER.warn("[diag] merge compare {}: {} x{} vs {} x{} patchA={} patchB={}",
                    verdict, a.getDescriptionId(), a.getCount(), b.getDescriptionId(), b.getCount(),
                    pa, pb);
            com.zaixiayesheng.ultrastacks.DiagLog.line(
                    "[diag] merge compare " + verdict + ": " + a.getDescriptionId() + " x"
                            + a.getCount() + " vs " + b.getDescriptionId() + " x" + b.getCount()
                            + " patchA=" + pa + " patchB=" + pb);
        }
    }

    /**
     * 让哈希与上面的放宽版 equals 自洽：哈希时忘掉 {@code max_stack_size}。
     *
     * <p>Makes hashing consistent with the relaxed equals above: the vanilla
     * {@code hashItemAndComponents} hashes the full merged component table, so a dirty stack
     * (patch=64) and a clean stack (default 65536) get different hashes and never merge inside
     * hash maps even when equals reports them equal. We hash every stack by its patch with
     * {@code MAX_STACK_SIZE} forgotten, so dirty and clean stacks land in the same bucket.</p>
     */
    @Inject(method = "hashItemAndComponents", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$hashForgetMax(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack == null) {
            // 原版 null → 0。
            // Vanilla returns 0 for null.
            cir.setReturnValue(0);
            return;
        }
        DataComponentPatch stripped = stack.getComponentsPatch()
                .forget(type -> type == DataComponents.MAX_STACK_SIZE);
        int i = 31 + stack.getItem().hashCode();
        cir.setReturnValue(31 * i + stripped.hashCode());
    }
}
