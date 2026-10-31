package com.zaixiayesheng.ultrastacks.core;

import com.zaixiayesheng.ultrastacks.DiagLog;
import java.util.Map;

/**
 * 卡顿看门狗：哪边超过 5 秒没动，就把那边的栈抄进日志。
 * 主线程卡住的时候自己报不了信，只能靠另一个线程替它说话。
     *
 * Stall watchdog: dumps the stack of whichever side stopped ticking for five seconds.
 * A stuck thread cannot report itself, so a daemon does it instead.
 */
public final class TickWatchdog {

    /** 超过这么久没心跳就当卡住。 */
    private static final long STALL_NANOS = 5_000_000_000L;

    /** 服务端最后一次 tick 的时间。 */
    private static volatile long serverBeat = System.nanoTime();

    private static volatile long clientBeat = System.nanoTime();

    private static volatile boolean started;

    private TickWatchdog() {
    }

    /** 服务端每 tick 调一次。 */
    public static void serverTick() {
        serverBeat = System.nanoTime();
    }

    /** 客户端每帧调一次。 */
    public static void clientTick() {
        clientBeat = System.nanoTime();
    }

    /** 起守护线程，重复调也只起一次。 */
    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        Thread thread = new Thread(TickWatchdog::loop, "ultrastacks-watchdog");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private static void loop() {
        while (true) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException interrupted) {
                return;
            }
            check("Server thread", true);
            check("Render thread", false);
        }
    }

    /** 看一边的心跳，停了就抄栈。 */
    private static void check(String threadName, boolean server) {
        long beat = server ? serverBeat : clientBeat;
        long idle = System.nanoTime() - beat;
        if (idle < STALL_NANOS) {
            return;
        }
        if (server) {
            serverBeat = System.nanoTime();
        } else {
            clientBeat = System.nanoTime();
        }
        StringBuilder text = new StringBuilder();
        text.append("[diag] stalled ").append(idle / 1_000_000L).append(" ms on ").append(threadName).append('\n');
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            if (!thread.getName().equals(threadName)) {
                continue;
            }
            text.append("    state=").append(thread.getState()).append('\n');
            StackTraceElement[] stack = entry.getValue();
            int limit = Math.min(stack.length, 60);
            for (int i = 0; i < limit; i++) {
                text.append("    at ").append(stack[i]).append('\n');
            }
        }
        DiagLog.line(text.toString());
    }
}
