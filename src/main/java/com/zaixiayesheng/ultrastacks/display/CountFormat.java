package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;

/**
 * 把原始数量变成显示模块要画的文本。
 *
 * <p>COMPACT 用 k / m / b 后缀缩写，EXACT 保留全部数字；低于 {@code abbreviateAbove} 的数量一律
 * 完整显示（该项最大 1000），所以正常游玩时看到的就是原版数字。
 *
 * <p>Turns a raw count into the text the display module draws. COMPACT abbreviates large numbers
 * with k / m / b suffixes, EXACT keeps every digit, and counts below {@code abbreviateAbove} (at most
 * 1000) are always printed in full, so normal gameplay shows the vanilla number.
 */
public final class CountFormat {

    /**
     * 仍然完整显示的计数上界：四位数字在槽位里放不下。
     *
     * <p>Largest count still drawn in full, since four digits do not fit in a slot.
     */
    private static final long MAX_PLAIN_COUNT = 999L;

    private CountFormat() {
    }

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
     * 用 {@code k} / {@code m} / {@code b} 后缀缩写；缩放后的数值不到十时保留一位小数。
     *
     * <p>Abbreviates with {@code k} / {@code m} / {@code b} suffixes, keeping one decimal while the scaled
     * value stays below ten.
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
