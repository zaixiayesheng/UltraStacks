package com.zaixiayesheng.ultrastacks.crafting;

/**
 * 批量合成的纯算术，不依赖 Minecraft 类型。
     *
 * <p>Pure arithmetic for bulk crafting, free of Minecraft types. Only two of the methods are used
 * by bulk crafting: {@link #capBySpace} caps the craft count by the free output space and
 * {@link #nextChunk} splits an oversized output into insertion chunks. Rounding and overflow, the
 * two things that silently create or destroy items, live here. The remaining methods are the same
 * arithmetic in pure form and have no caller at the moment.
 */
public final class CraftingMath {

    private CraftingMath() {
    }

    /**
     * 三个来源的可用量之和，各自先夹到非负，避免一个负数把总量拉低。
     *
     * <p>Sum of the three supplies, each clamped to non-negative first so one negative cannot drag
     * the total down.
     */
    public static long available(long grid, long storage, long inventory) {
        return Math.max(0, grid) + Math.max(0, storage) + Math.max(0, inventory);
    }

    /**
     * 平均值：各格数量相等时才成立；生产路径按逐格最小值算，不经过这里。
     *
     * <p>Average: only valid when every slot holds the same count. The production path uses the
     * per-slot minimum instead and never calls this.
     */
    public static int crafts(long available, int slotsUsing) {
        if (slotsUsing <= 0) {
            return 0;
        }
        long value = available / slotsUsing;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    /**
     * 按产物空位截断合成次数：一次合成产出 {@code perCraft} 个，空位放得下几次就只能合几次。
     *
     * <p>Caps the craft count by the free output space: one craft yields {@code perCraft} items, so the
     * number of crafts is limited to what the free space still holds.
     */
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
     * 单次插入的数量：不超过一摞，也不超过剩余量。
     *
     * <p>Size of one insertion: at most one stack and at most what is left. A single
     * {@code ItemStack} can never hold more than its own limit, so an output larger than one stack
     * has to be inserted in chunks or the excess would be dropped silently.
     */
    public static int nextChunk(long remaining, int maxStackSize) {
        if (remaining <= 0 || maxStackSize <= 0) {
            return 0;
        }
        return (int) Math.min(remaining, maxStackSize);
    }

    /**
     * 放下 {@code total} 需要几次插入，向上取整；与 {@link #nextChunk} 配套，目前没有调用点。
     *
     * <p>How many insertions {@code total} needs, rounded up. It belongs with {@link #nextChunk} and
     * has no caller at the moment.
     */
    public static int chunkCount(long total, int maxStackSize) {
        if (total <= 0 || maxStackSize <= 0) {
            return 0;
        }
        return (int) ((total + maxStackSize - 1) / maxStackSize);
    }
}
