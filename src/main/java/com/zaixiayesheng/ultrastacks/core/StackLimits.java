package com.zaixiayesheng.ultrastacks.core;

/**
 * 堆叠上限的纯算术，不碰任何 Minecraft 类型。
 * Pure stack-limit arithmetic; no Minecraft types involved.
 */
public final class StackLimits {
    /**
     * 等比换算的基准：原版 64 个一堆。
     *
     * <p>Reference for proportional scaling: vanilla's 64 items per stack.
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
            // 上限不超过 1 的物品（工具、盔甲、桶）与空槽的默认值都原样返回。
            return vanillaMax <= 0 ? 1 : vanillaMax;
        }

        if (!scaleSmallItems) {
            return Math.max(vanillaMax, targetMax);
        }

        long scaled = (long) vanillaMax * targetMax / VANILLA_REFERENCE;
        return (int) Math.max(1L, Math.min((long) targetMax, scaled));
    }

    /**
     * 把请求的上界夹到编解码器认的范围（原版只到 99）。目前没调用点，留着当以后改数字的安全阀。
     * Clamps a requested bound to what the codec accepts; no caller yet, kept as a safety valve.
     */
    public static int codecBound(int requested) {
        return Math.max(99, requested);
    }
}
