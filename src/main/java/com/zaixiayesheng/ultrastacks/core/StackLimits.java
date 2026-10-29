package com.zaixiayesheng.ultrastacks.core;

/**
 * 堆叠上限的纯算术，不依赖任何 Minecraft 类型。
 *
 * <p>按原版 64 的基准做等比换算（见 {@link #resolve}），另给出编解码用的兜底下界（见 {@link #codecBound}）。
 *
 * <p>Pure stack-size arithmetic that touches no Minecraft type: proportional scaling against the
 * vanilla 64 reference in {@link #resolve}, plus the codec floor in {@link #codecBound}.
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
     * 算出某个物品的新上限。
     *
     * <p>原版上限不超过 1 的物品（工具、空槽的默认值）原样返回，非正值按 1 处理；不开等比换算时取
     * 两者较大值；开启时按 {@code 原版上限 × 目标上限 / 64} 缩放并夹在 1 与目标上限之间，所以 16 个
     * 一堆的物品只拿到目标值的四分之一。乘法先转 {@code long}，大上限下也不会溢出。
     *
     * <p>Computes the new limit for one item. Items whose original limit is at most 1 (tools, empty
     * slot defaults) are returned unchanged, with non-positive values treated as 1. Without
     * proportional scaling the larger of the two limits wins; with it the limit is
     * {@code original × target / 64}, clamped between 1 and the target, so a 16-size item receives
     * a quarter of the target. The multiplication widens to {@code long} first, so it cannot
     * overflow at large limits.
     *
     * @param vanillaMax 该物品原本的上限（64、16、1……）。The item's original limit (64, 16, 1, ...).
     * @param targetMax 配置给 64 个一堆的物品的上限。The configured limit for 64-size items.
     * @param scaleSmallItems 是否让小堆叠按比例缩小。Whether smaller stacks scale down proportionally.
     * @return 新上限，最小为 1。The new limit, never below 1.
     */
    public static int resolve(int vanillaMax, int targetMax, boolean scaleSmallItems) {
        if (vanillaMax <= 1) {
            // 不可堆叠的物品（工具、盔甲等）与非法默认值都保持原样。
            return vanillaMax <= 0 ? 1 : vanillaMax;
        }

        if (!scaleSmallItems) {
            return Math.max(vanillaMax, targetMax);
        }

        long scaled = (long) vanillaMax * targetMax / VANILLA_REFERENCE;
        return (int) Math.max(1L, Math.min((long) targetMax, scaled));
    }

    /**
     * 把请求的上界夹到编解码器能接受的范围。
     *
     * <p>原版编解码器只认到 99，低于 99 的请求会让原版堆叠也放不下，所以返回值不小于 99。
     * 目前没有调用点，留着作为以后改数字时的安全出口。
     *
     * <p>Clamps a requested upper bound to what the codec accepts. The vanilla codec only allows 99,
     * so a smaller request would break even vanilla stacks; the result is never below 99. It has
     * no caller right now and stays as a safe exit for future number changes.
     */
    public static int codecBound(int requested) {
        return Math.max(99, requested);
    }
}
