package com.zaixiayesheng.ultrastacks.core;

/**
 * 堆叠上限的纯算术，不碰任何 Minecraft 类型。
 * Pure stack-limit arithmetic; no Minecraft types involved.
 */
public final class StackLimits {
    /**
     * 原版基准上限，按比例缩放小堆叠时当分母用。
     *
     * <p>The vanilla reference limit, used as the denominator when scaling smaller stacks.
     */
    public static final int VANILLA_REFERENCE = 64;

    private StackLimits() {
    }

    /**
     * 算某个物品缩放后的上限：≤1 原样返回，不开等比就取较大值，开了就按 64 等比缩放并夹在 1..目标值。
     * 乘法先转 long，上限再大也不溢出。
     *
     * Scaled limit for one item: ≤1 unchanged, otherwise the larger value or a
     * 64-based scale clamped to 1..target. Multiplies as long, so no overflow.
     */
    public static int resolve(int vanillaMax, int targetMax, boolean scaleSmallItems) {
        if (vanillaMax <= 1) {
            // Non-stackable (tools, armour, buckets) and empty defaults stay untouched.
            return vanillaMax <= 0 ? 1 : vanillaMax;
        }

        if (!scaleSmallItems) {
            return Math.max(vanillaMax, targetMax);
        }

        long scaled = (long) vanillaMax * targetMax / VANILLA_REFERENCE;
        return (int) Math.max(1L, Math.min((long) targetMax, scaled));
    }

    /**
     * 存档与网络格式能承载的上限。
     *
     * <p>The bound that the save and network formats can carry.
     */
    public static int codecBound(int requested) {
        return Math.max(99, requested);
    }
}
