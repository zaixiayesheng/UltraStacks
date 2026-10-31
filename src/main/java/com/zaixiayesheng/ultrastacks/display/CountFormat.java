package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;

/**
 * 把原始数量换算成显示模块要画的文本。
     *
 * <p>Turns a raw count into the text the display module draws.
 */
public final class CountFormat {

    /**
     * 仍然完整显示的最大数量。
     *
     * <p>The largest count still drawn in full.
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
     * 用 k / m / b 缩写；缩完的整数部分小于十时保留一位小数。
     *
     * <p>Abbreviates with k / m / b, keeping one decimal while the whole part is below ten.
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
