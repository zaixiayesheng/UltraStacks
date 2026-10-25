package com.zaixiayesheng.ultrastacks.crafting;

/**
 * 批量合成的纯算术，不依赖 Minecraft 类型。
 *
 * <p>生产路径只用到两个：按产物空位截断合成次数（{@link #capBySpace}），把超过单堆上限的产物拆成
 * 多次插入（{@link #nextChunk}）。其余几个（{@link #available}、{@link #crafts}、
 * {@link #expectedConsumed}、{@link #expectedProduced}、{@link #balanced}、{@link #chunkCount}）
 * 是同一套换算的备用件，本树里还没有调用点。取整、溢出这些会凭空多出或少掉物品的换算都集中在这里。
 *
 * <p>Pure arithmetic for bulk crafting that touches no Minecraft type. The production path uses two
 * of these: {@link #capBySpace} caps the craft count by free output space, and {@link #nextChunk}
 * splits an oversized result into insertion chunks. The rest ({@link #available}, {@link #crafts},
 * {@link #expectedConsumed}, {@link #expectedProduced}, {@link #balanced}, {@link #chunkCount}) are
 * spare parts of the same arithmetic with no caller in this tree. Rounding and overflow, the
 * conversions that could silently create or destroy items, all live here.
 */
public final class CraftingMath {

    private CraftingMath() {
    }

    public static long available(long grid, long storage, long inventory) {
        return Math.max(0, grid) + Math.max(0, storage) + Math.max(0, inventory);
    }

    /**
     * 平均值：{@code available / slotsUsing}，只在各格数量相等时成立。生产路径按逐格最小值算，
     * 不走这个方法；本树里也没有调用点。
     *
     * <p>The average, {@code available / slotsUsing}, which only holds when every slot holds the same
     * count. The production path takes the per-slot minimum instead and does not use this, and nothing
     * calls it in this tree.
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
     * 批量产物下一次插入的块大小。
     *
     * <p>一个 ItemStack 装不下超过自身上限的数量，一次塞超出的部分会被静默丢掉，所以按上限切块。
     *
     * <p>Size of the next insertion chunk for a bulk output. A single ItemStack can never hold more than
     * its own max stack size, and the excess would be dropped silently, so the total is cut into chunks
     * of at most that size.
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
