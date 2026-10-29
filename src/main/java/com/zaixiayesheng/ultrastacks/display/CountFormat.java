package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;

/**
 * 把原始数量换算成显示模块要画的文本。
 *
 * <p>COMPACT 用 k / m / b 后缀缩写，EXACT 保留每一位数字；低于 {@code abbreviateAbove} 的数量一律
 * 完整显示，所以正常游玩时看到的仍是原版数字。显示模块关闭时也走完整数字，等于把绘制还给原版。
 *
 * <p>Turns a raw count into the text the display module draws.
 *
 * <p>COMPACT abbreviates with the k / m / b suffixes and EXACT keeps every digit; counts below
 * {@code abbreviateAbove} are always printed in full, so ordinary gameplay still shows vanilla numbers.
 * With the display module off the full number is used as well, which hands the drawing back to vanilla.
 */
public final class CountFormat {

    /**
     * 仍然完整显示的最大数量。
     *
     * <p>四位数在 16 像素宽的槽位里放不下，会被裁掉一截，所以上限是三位数 999。
     *
     * <p>The largest count still drawn in full.
     *
     * <p>Four digits do not fit the sixteen-pixel width of a slot and would be cut off, so the ceiling
     * is the three-digit 999.
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
     * <p>例如 1234 得到 {@code 1.2k}，12345 得到 {@code 12k}：一位小数只在数字本身很短时才有意
     * 义，位数多了反而占地方。
     *
     * <p>Abbreviates with k / m / b, keeping one decimal while the whole part is below ten.
     *
     * <p>1234 becomes {@code 1.2k} and 12345 becomes {@code 12k}: a decimal only adds information while
     * the number itself is short, and only takes up room once it is not.
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
