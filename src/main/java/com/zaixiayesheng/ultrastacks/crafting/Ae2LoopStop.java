package com.zaixiayesheng.ultrastacks.crafting;

import java.util.concurrent.atomic.AtomicLong;

/**
 * AE 循环内的次数闸：每次取料记一次，数满就把取料判成“材料不足”，AE 会自己退出循环。
 * 用静态计数而不是 ThreadLocal —— 之前那版按线程存，实测数不到 64，说明取料并非都落在同一线程上。
 * 空闲 250 毫秒归零，所以一次 Shift 一个窗口。
 *
 * <p>Per-burst gate for AE's internal loop. Every ingredient extraction counts once; once the cap is
 * reached the extraction reports "no material" and AE stops by itself. A plain static counter is used
 * instead of a ThreadLocal because the previous build never reached the cap — the extraction does not
 * always run on the same thread. The window resets after 250 ms of inactivity.
 */
public final class Ae2LoopStop {
    private static final long WINDOW_NS = 250_000_000L;
    private static final AtomicLong COUNT = new AtomicLong();
    private static final AtomicLong LAST = new AtomicLong();
    private static volatile boolean unlocked;

    private Ae2LoopStop() {
    }

    public static void setUnlocked(boolean value) {
        unlocked = value;
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static void reset() {
        COUNT.set(0L);
        LAST.set(0L);
    }

    /** @return true 当这一轮的次数已经用满 */
    public static boolean over(int cap) {
        long now = System.nanoTime();
        long last = LAST.get();
        if (now - last > WINDOW_NS) {
            COUNT.set(0L);
        }
        LAST.set(now);
        return !unlocked && COUNT.get() >= cap;
    }

    /** 空闲超过窗口就开新一轮：不清这一步的话，上一轮的大数字会把后面所有合成卡死。 */
    public static boolean touch() {
        long now = System.nanoTime();
        long last = LAST.get();
        boolean fresh = now - last > WINDOW_NS;
        if (fresh) {
            COUNT.set(0L);
        }
        LAST.set(now);
        return fresh;
    }

    public static void record() {
        COUNT.incrementAndGet();
    }

    public static long count() {
        return COUNT.get();
    }
}
