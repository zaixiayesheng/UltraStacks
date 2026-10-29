package com.zaixiayesheng.ultrastacks.core;

/**
 * 不依赖 Minecraft 类型的堆叠尺寸纯计算。
 *
 * <p>把「配置里填的目标上限」换算成「某个物品实际的堆叠上限」的规则全在这里，因为这段算术最容易出
 * 溢出和取整错误，单独拎出来才能脱离游戏环境验证。
 *
 * <p>Pure stack-size arithmetic that does not depend on any Minecraft type.
 *
 * <p>All the rules for turning the configured target limit into the actual limit of one particular item
 * live here, because this arithmetic is exactly where overflow and rounding mistakes hide; keeping it
 * separate makes it testable outside the game.
 */
public final class StackLimits {
    /**
     * 原版基准上限，按比例缩放小堆叠时当分母用。
     *
     * <p>缩放公式以「64 个的堆叠放大到目标上限」为基准，因此 16 个的堆叠会拿到目标上限的四分之一。
     *
     * <p>The vanilla reference limit, used as the denominator when scaling smaller stacks.
     *
     * <p>The scaling formula treats a 64-size stack as growing to the target limit, so a 16-size stack
     * ends up with a quarter of that target.
     */
    public static final int VANILLA_REFERENCE = 64;

    private StackLimits() {
    }

    /**
     * 算出某个物品在新规则下的堆叠上限。
     *
     * <p>上限本就小于等于 1 的物品（工具、护甲、桶这类不可堆叠物，以及默认值 0）保持原样，只有 0 会被
     * 抬到 1，避免出现「上限 0 个」这种无法放进槽位的状态。关闭 {@code scaleSmallItems} 时取原上限与
     * 目标上限的较大者，因此不会把任何物品改小；开启时按 {@link #VANILLA_REFERENCE} 等比缩放，结果夹在
     * 1 与目标上限之间。
     *
     * <p>Resolves the stack limit an item gets under the new rules.
     *
     * <p>Items whose limit is already 1 or less — tools, armour, buckets and other unstackables, plus
     * the default 0 — keep their limit; only 0 is raised to 1, so that no item can end up with a limit
     * of zero and become impossible to place in a slot. With {@code scaleSmallItems} off the result is
     * the larger of the original limit and the target, so nothing is ever shrunk; with it on the limit
     * is scaled in proportion to {@link #VANILLA_REFERENCE} and clamped between 1 and the target.
     *
     * @param vanillaMax 该物品原本的上限（64、16、1 等） / the item's original limit (64, 16, 1, ...)
     * @param targetMax 64 个堆叠的目标上限 / the configured limit for 64-size items
     * @param scaleSmallItems 为 true 时小堆叠按比例保留相对大小 / when true, smaller stacks keep their relative size
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
     * <p>原版自己的堆叠上限是 99，如果把这个界压到 99 以下，原版原有的 99 个堆叠就无法存盘，
     * 所以返回值永不小于 99。
     *
     * <p>The bound that the save and network formats can carry.
     *
     * <p>Vanilla's own stack limit is 99; pushing this bound below 99 would make an ordinary vanilla
     * 99-item stack impossible to save, so the result is never smaller than 99.
     */
    public static int codecBound(int requested) {
        return Math.max(99, requested);
    }
}
