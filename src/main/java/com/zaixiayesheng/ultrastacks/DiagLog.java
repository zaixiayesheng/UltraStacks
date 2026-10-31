package com.zaixiayesheng.ultrastacks;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 诊断用的日志，单独写一个文件。
     *
 * <p>Minecraft's own logs sit behind log4j buffering and stay empty until the client exits, which
 * is useless when you need to look right now. This writes to {@code config/ultrastacks-diag.log}
 * and flushes every line.</p>
 */
public final class DiagLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final AtomicBoolean INIT = new AtomicBoolean(false);
    private static final java.util.Set<String> SEEN = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static FileWriter writer;

    private DiagLog() {
    }

    private static void init() {
        if (INIT.get()) {
            return;
        }
        synchronized (DiagLog.class) {
            if (INIT.get()) {
                return;
            }
            try {
                Path path = FMLPaths.CONFIGDIR.get().resolve("ultrastacks-diag.log");
                writer = new FileWriter(path.toFile(), true);
            } catch (IOException ignored) {
                writer = null;
            }
            INIT.set(true);
        }
    }

    /**
     * 写一行，立刻落盘。写不进去就算了，诊断不能反过来把游戏搞坏。
     *
     * <p>Writes one line and flushes; failures are swallowed on purpose.
     */
    public static synchronized void line(String message) {
        init();
        if (writer == null) {
            return;
        }
        try {
            writer.write("[" + LocalTime.now().format(TIME) + "] " + message + System.lineSeparator());
            writer.flush();
        } catch (IOException ignored) {
            // 诊断通道损坏时放弃写入，不影响游戏。
        }
    }

    /**
     * 同一个 key 只写一次，用来记录那些会反复触发、但只想看一次的现场。
     *
     * <p>Writes a line once per key. Handy for values that fire constantly but only need to be
     * seen the first time.</p>
     */
    public static void lineThrottled(String key, String message) {
        if (!SEEN.add(key)) {
            return;
        }
        line(message);
    }
}
