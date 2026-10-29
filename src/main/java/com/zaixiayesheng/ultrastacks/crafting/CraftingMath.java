package com.zaixiayesheng.ultrastacks.crafting;

/**
 * 批量合成的纯算术，不依赖 Minecraft 类型。
 *
 * <p>生产路径只用到两件事：按产物空位截断合成次数（{@link #capBySpace}），把超过单堆上限的产物拆成
 * 多次插入（{@link #nextChunk}）。取整与溢出这类会凭空多出或吃掉物品的换算都集中在这里；其余几个方法
 * 供自检用，不在合成路径上。
 *
 * <p>Pure arithmetic for bulk crafting, independent of any Minecraft type.
 *
 * <p>The production path uses only two things: capping the craft count by the free output space
 * ({@link #capBySpace}) and splitting an oversized output into several insertions ({@link #nextChunk}).
 * All the rounding and overflow arithmetic that could invent or swallow items is concentrated here; the
 * remaining methods are self-check helpers and are not on the crafting path.
 */
public final class CraftingMath {

    private CraftingMath() {
    }

    public static long available(long grid, long storage, long inventory) {
        return Math.max(0, grid) + Math.max(0, storage) + Math.max(0, inventory);
    }

    /**
     * 用总料量除以参与格数得到平均可合次数。
     *
     * <p>这个结果只在各格数量完全相等时才成立，所以不在合成路径上：真正的次数由调用方逐格取最小值得到。
     * 除以 0 或负数返回 0，结果再夹在 0 与 {@link Integer#MAX_VALUE} 之间。
     *
     * <p>Divides the total ingredient count by the number of slots in use to get an average craft count.
     *
     * <p>The result only holds while every slot holds exactly the same amount, which is why this is not
     * on the crafting path: the real count comes from taking the per-slot minimum. A non-positive slot
     * count yields 0, and the result is clamped between 0 and {@link Integer#MAX_VALUE}.
     */
    public static int crafts(long available, int slotsUsing) {
        if (slotsUsing <= 0) {
            return 0;
        }
        long value = available / slotsUsing;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    public static int capBySpace(int crafts, long freeSpace, int perCraft) {
        if (perCraft <= 0 || freeSpace <= 0) {
            return 0;
        }
        return Math.min(crafts, (int) Math.min(Integer.MAX_VALUE, freeSpace / perCraft));
    }

    public static long expectedConsumed(int crafts, int occupiedSlots) {
        return (long) crafts * Math.max(0, occupiedSlots);
    }

    public static long expectedProduced(int crafts, int perCraft) {
        return (long) crafts * Math.max(0, perCraft);
    }

    public static boolean balanced(long consumed, int crafts, int occupiedSlots) {
        return consumed == expectedConsumed(crafts, occupiedSlots);
    }

    /**
     * 批量产物下一次插入该用多大一块。
     *
     * <p>单个 {@code ItemStack} 永远装不下超过它自身上限的数量，所以超出的部分必须拆开插入，否则会被
     * 静默丢掉。剩余量或上限非正时返回 0，调用方据此结束循环。
     *
     * <p>How large the next insertion chunk of a bulk output should be.
     *
     * <p>A single {@code ItemStack} can never hold more than its own limit, so anything larger has to be
     * split across insertions or the excess is silently dropped. A non-positive remaining count or limit
     * yields 0, which tells the caller to stop looping.
     */
    public static int nextChunk(long remaining, int maxStackSize) {
        if (remaining <= 0 || maxStackSize <= 0) {
            return 0;
        }
        return (int) Math.min(remaining, maxStackSize);
    }

    public static int chunkCount(long total, int maxStackSize) {
        if (total <= 0 || maxStackSize <= 0) {
            return 0;
        }
        return (int) ((total + maxStackSize - 1) / maxStackSize);
    }
}
