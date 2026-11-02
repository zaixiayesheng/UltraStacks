package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * 把数量转成显示模块要画的文本。
 *
 * <p>格式与精妙（Sophisticated Storage/Core）的 {@code CountAbbreviator} 完全一致，这样同一屏里两边的
 * 数字看起来就是一套：四位以内直接写数字（超过一千带千位逗号），更多位按 k / m / b 缩写、最多四个字符、
 * 小数向下取整。
 *
 * <p>示例：{@code 999 → 999}、{@code 1234 → 1,234}、{@code 16379 → 16.3k}、{@code 65536 → 65.5k}、
 * {@code 999999 → 999k}、{@code 1000000 → 1.00m}、{@code 2147483647 → 2.14b}。
 *
 * <p>格式化与精妙的 {@code CountAbbreviator} 完全一致：最多四个字符，多余部分按 k / m / b 缩写，小数
 * 一律向下取整。这样同一界面里两边的数字读起来是同一套写法。
 *
 * <p>Formats counts exactly like Sophisticated's {@code CountAbbreviator} so both mods' numbers read
 * the same on one screen: up to four characters printed as digits (thousands separated by a comma),
 * everything larger abbreviated to k / m / b, decimals floored.
 */
public final class CountFormat {

    private static final String[] SUFFIXES = {"k", "m", "b"};

    /** 缩写后允许的最大字符数，与精妙的默认值一致。 / Character budget after abbreviation. */
    private static final int MAX_CHARACTERS = 4;

    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(Locale.ROOT);

    /** 两位小数、向下取整。 / Two decimals, floored. */
    private static final DecimalFormat TWO_DECIMALS = new DecimalFormat("#.00", SYMBOLS);

    /** 一位小数、向下取整。 / One decimal, floored. */
    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("##.0", SYMBOLS);

    static {
        TWO_DECIMALS.setRoundingMode(RoundingMode.DOWN);
        ONE_DECIMAL.setRoundingMode(RoundingMode.DOWN);
    }

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
        long threshold = Math.min(UltraStacksConfig.abbreviateAbove(), 1_000L);
        if (!UltraStacksConfig.displayModule()
                || UltraStacksConfig.countStyle() == UltraStacksConfig.CountStyle.EXACT
                || count < threshold) {
            return Long.toString(count);
        }
        return abbreviate(count);
    }

    /**
     * 与精妙相同的缩写：四位以内写数字，更多位按 k / m / b、最多四个字符、小数向下取整。
     *
     * <p>Same abbreviation Sophisticated uses: digits up to four characters, then k / m / b with at
     * most four characters and floored decimals.
     */
    public static String abbreviate(long count) {
        if (count < 1_000L) {
            return Long.toString(count);
        }
        int digits = (int) Math.log10(count) + 1;
        if (digits <= MAX_CHARACTERS) {
            return String.format(Locale.ROOT, "%,d", count);
        }
        int exponent = (digits - MAX_CHARACTERS) / 3 + 1;
        if (exponent > SUFFIXES.length) {
            exponent = SUFFIXES.length;
        }
        String suffix = SUFFIXES[exponent - 1];
        double divided = count / Math.pow(1000.0, exponent);
        int wholeDigits = digits - exponent * 3;
        int precision = MAX_CHARACTERS - 1 - wholeDigits;
        String number;
        if (wholeDigits > 3 || precision <= 0) {
            number = String.format(Locale.ROOT, "%,d", (long) divided);
        } else if (precision == 2) {
            number = TWO_DECIMALS.format(divided);
        } else {
            number = ONE_DECIMAL.format(divided);
        }
        return number + suffix;
    }
}
