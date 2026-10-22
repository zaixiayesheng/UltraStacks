package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;

/**
 * Turns a raw count into the text the display module draws.
 *
 * <p>COMPACT abbreviates large numbers with k / m / b suffixes; EXACT keeps every digit. Counts
 * below {@code abbreviateAbove} are always printed in full, so normal gameplay is unchanged.
 *
 * <p><b>中文</b> 显示模块的数字格式：COMPACT 用 k / m / b 缩写，EXACT 显示完整数字；
 * 低于 {@code abbreviateAbove} 的数量一律显示完整值，正常游玩时看到的就是原版数字。
 */
public final class CountFormat {

    private CountFormat() {
    }

    /** Formats {@code count} according to the current display config. */
    public static String format(long count) {
        if (count < 0) {
            return Long.toString(count);
        }
        if (!UltraStacksConfig.displayModule()
                || UltraStacksConfig.countStyle() == UltraStacksConfig.CountStyle.EXACT
                || count < UltraStacksConfig.abbreviateAbove()) {
            return Long.toString(count);
        }
        return abbreviate(count);
    }

    /** Abbreviates with k / m / b; one decimal is kept while the scaled value is below ten. */
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
