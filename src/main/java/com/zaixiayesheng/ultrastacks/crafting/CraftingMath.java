package com.zaixiayesheng.ultrastacks.crafting;

/**
 * 批量合成的纯算术，不依赖 Minecraft 类型。
     *
 * <p>Pure arithmetic for bulk crafting, independent of any Minecraft type.
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
     * <p>Divides the total ingredient count by the number of slots in use to get an average craft count.
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
     * <p>How large the next insertion chunk of a bulk output should be.
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
