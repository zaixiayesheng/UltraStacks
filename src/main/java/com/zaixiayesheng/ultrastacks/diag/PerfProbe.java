package com.zaixiayesheng.ultrastacks.diag;

import com.zaixiayesheng.ultrastacks.DiagLog;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 临时性能探针：只统计，不改任何行为。
 *
 * <p>Temporary performance probe. It only records how often the crafting/terminal entry points run
 * and how long they take, then flushes a summary line every few seconds. Nothing here changes what
 * the game does — delete the probe mixins once the numbers are in.
 *
 * <p>Writes go to {@code config/ultrastacks-diag.log} through {@link DiagLog}; the console stays quiet.
 */
public final class PerfProbe {

    /** 汇总窗口长度（毫秒）。 / Summary window length in milliseconds. */
    private static final long WINDOW_MS = 2000L;

    /** 单次超过这个耗时就单独记一行（纳秒）。 / Anything slower than this is logged on its own. */
    private static final long SLOW_NS = 15_000_000L;

    /** key → {调用次数, 总纳秒}。 / key → {calls, total nanos}. */
    private static final Map<String, long[]> STATS = new ConcurrentHashMap<>();

    private static volatile long lastFlush = System.currentTimeMillis();

    /** 每个线程当前这次调用的起始时间。 / Start time of the call being measured on this thread. */
    private static final ThreadLocal<Long> START = new ThreadLocal<>();

    /**
     * 每个线程、每个 key 各自的计时栈。
     *
     * <p>Per-thread stack of start times, one stack per key. Nested calls reuse the same key (for
     * example a crafting slot calling into itself) push and pop independently, so an inner call can
     * never overwrite the outer call's start time the way a single shared slot would.
     */
    private static final ThreadLocal<Map<String, Deque<Long>>> STACKS =
            ThreadLocal.withInitial(HashMap::new);

    private PerfProbe() {
    }

    /** 压入开始时间。 / Push the start of a measured call. */
    public static void push(String key) {
        STACKS.get().computeIfAbsent(key, k -> new ArrayDeque<>()).push(System.nanoTime());
    }

    /** 弹出并结算这一个 key 最近压入的时间。 / Pop and record the most recent entry for this key. */
    public static void pop(String key) {
        Deque<Long> stack = STACKS.get().get(key);
        if (stack == null || stack.isEmpty()) {
            return;
        }
        timing(key, System.nanoTime() - stack.pop());
    }

    /** 开始计时。 / Mark the start of a measured call. */
    public static void mark() {
        START.set(System.nanoTime());
    }

    /** 结算上一次 {@link #mark()} 到现在，并记入 key。 / Close the mark and record it. */
    public static void since(String key) {
        Long start = START.get();
        if (start != null) {
            START.remove();
            timing(key, System.nanoTime() - start);
        }
    }

    /** 该 key 的平均单次耗时（纳秒）；没有数据返回 0。 / Average nanos per call for a key, 0 when unknown. */
    public static long averageNanos(String key) {
        long[] stat = STATS.get(key);
        if (stat == null || stat[0] <= 0L) {
            return 0L;
        }
        return stat[1] / stat[0];
    }

    /** 记一次调用。 / Count one call. */
    public static void count(String key) {
        STATS.computeIfAbsent(key, k -> new long[2])[0]++;
        maybeFlush();
    }

    /** 记一次带耗时的调用。 / Count one timed call. */
    public static void timing(String key, long nanos) {
        long[] stat = STATS.computeIfAbsent(key, k -> new long[2]);
        synchronized (stat) {
            stat[0]++;
            stat[1] += nanos;
        }
        if (nanos >= SLOW_NS) {
            DiagLog.line("[diag] perf slow: " + key + " " + (nanos / 1_000_000.0) + " ms");
        }
        maybeFlush();
    }

    private static void maybeFlush() {
        long now = System.currentTimeMillis();
        if (now - lastFlush < WINDOW_MS) {
            return;
        }
        lastFlush = now;
        flush();
    }

    /** 把窗口内的统计写一行。 / Write one summary line for the finished window. */
    public static synchronized void flush() {
        if (STATS.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("[diag] perf 2s:");
        STATS.forEach((key, stat) -> {
            long calls = stat[0];
            long nanos = stat[1];
            sb.append(' ').append(key).append('=').append(calls);
            if (nanos > 0L) {
                sb.append('/').append(Math.round(nanos / 1000.0 / Math.max(1L, calls))).append("us");
            }
        });
        STATS.clear();
        DiagLog.line(sb.toString());
    }
}
