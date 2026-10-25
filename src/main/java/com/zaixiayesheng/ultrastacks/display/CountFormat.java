package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;

/**
 * 把数量转成显示模块要画的文本。
 *
 * <p>{@code COMPACT} 用 k / m / b 缩写，{@code EXACT} 保留每一位数字；小于阈值（{@code abbreviateAbove}，
 * 最多 1000）的数量一律显示完整值，所以正常游玩时看到的就是原版数字。
 *
 * <p>Turns a raw count into the text the display module draws. {@code COMPACT} abbreviates with
 * k / m / b suffixes, {@code EXACT} keeps every digit, and any count below the threshold
 * ({@code abbreviateAbove}, at most 1000) is printed in full, so ordinary gameplay shows the
 * vanilla number.
 */
public final class CountFormat {

    /**
     * 仍然完整显示的最大数量，也是缩写阈值的上限。原版一摞最多 99 个，三位数字放得下，四位就会挤出槽位。
     *
     * <p>Largest count still printed in full, and the cap on the abbreviation threshold. A vanilla
     * stack holds at most 99 items, so three digits fit a slot while four spill out of it.
     */
    private static final long MAX_PLAIN_COUNT = 999L;

    private CountFormat() {
    }

    /**
     * 按配置决定用完整数字还是缩写。
     *
     * <p>负数原样输出；显示模块关闭、样式为 {@code EXACT}，或数量小于阈值时返回完整数字。
     *
     * <p>Chooses between full digits and an abbreviation according to the config. Negative counts
     * are printed as they are; full digits are returned when the display module is off, the style
     * is {@code EXACT}, or the count is below the threshold.
     */
    public static String format(long count) {
        if (count < 0) {
            return Long.toString(count);
        }
        long threshold = Math.min(UltraStacksConfig.abbreviateAbove(), MAX_PLAIN_COUNT + 1L);
        if (!UltraStacksConfig.displayModule()
                || UltraStacksConfig.countStyle() == UltraStacksConfig.CountStyle.EXACT
                || count < threshold) {
            return Long.toString(count);
        }
        return abbreviate(count);
    }

    /**
     * 按千进制缩写：k、m、b；缩放后小于十时保留一位小数，例如 {@code 1500 → 1.5k}。
     *
     * <p>Abbreviates by thousands with k, m and b, keeping one decimal while the scaled value stays
     * below ten, for example {@code 1500 → 1.5k}.
     */
    public static String abbreviate(long count) {
        if (count < 1_000L) {
            return Long.toString(count);
        }
        if (count < 1_000_000L) {
            return scaled(count, 1_000L, "k");
        }
        if (count < 1_000_000_000L) {
            return scaled(count, 1_000_000L, "m");
        }
        return scaled(count, 1_000_000_000L, "b");
    }

    private static String scaled(long count, long divisor, String suffix) {
        long whole = count / divisor;
        if (whole < 10L) {
            long tenth = (count % divisor) * 10L / divisor;
            return tenth == 0L ? whole + suffix : whole + "." + tenth + suffix;
        }
        return whole + suffix;
    }
}
