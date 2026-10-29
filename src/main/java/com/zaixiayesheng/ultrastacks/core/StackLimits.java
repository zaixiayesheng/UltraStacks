package com.zaixiayesheng.ultrastacks.core;

/**
 * 不依赖任何 Minecraft 类型的堆叠尺寸纯计算。
 *
 * <p>按原版 64 的基准做等比换算（见 {@link #resolve}），另给一个兜底下界（见 {@link #codecBound}）。
 *
 * <p>Pure stack-size arithmetic that touches no Minecraft type: proportional scaling against the
 * vanilla 64 reference in {@link #resolve}, plus a lower bound in {@link #codecBound}.
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
     * 算出某个物品缩放后的堆叠上限。
     *
     * <p>原版上限不超过 1 的物品（工具、盔甲、空槽的默认值）原样返回，非正值按 1 处理；不开等比换算时取
     * 两者较大值；开启时按 {@code 原版上限 × 目标上限 / 64} 缩放并夹在 1 与目标上限之间，所以 16 个一堆
     * 的物品只拿到目标值的四分之一。乘法先转 {@code long}，上限很大时也不会溢出。
     *
     * <p>Computes the scaled stack limit for one item. Items whose original limit is at most one (tools,
     * armour, empty-slot defaults) are returned unchanged, with non-positive values treated as one.
     * Without proportional scaling the larger of the two limits wins; with it the limit is
     * {@code original × target / 64}, clamped between one and the target, so a 16-size item gets a
     * quarter of the target. The multiplication widens to {@code long} first, so it cannot overflow at
     * large limits.
     *
     * @param vanillaMax 该物品原本的上限（64、16、1……）。The item's original limit (64, 16, 1, ...).
     * @param targetMax 配置给 64 个一堆的物品的上限。The configured limit for 64-size items.
     * @param scaleSmallItems 是否让小堆叠按比例缩放。Whether smaller stacks scale proportionally.
     * @return 新上限，最小为 1。The new limit, never below one.
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
     * 把请求的上界夹到不小于 99。
     *
     * <p>99 是若干模组写死的槽位上限，低于它的请求会把那些槽位改小，所以返回值不小于 99。
     * 本树里目前没有调用点，留着作为以后改数字时的安全出口。
     *
     * <p>Clamps a requested upper bound to at least 99. That value is the slot ceiling several mods
     * hardcode, and a smaller request would shrink those slots, so the result is never below 99.
     * Nothing calls it in this tree yet; it stays as a safety exit for future number changes.
     */
    public static int codecBound(int requested) {
        return Math.max(99, requested);
    }
}
