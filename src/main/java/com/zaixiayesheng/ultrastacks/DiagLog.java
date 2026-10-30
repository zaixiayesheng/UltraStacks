package com.zaixiayesheng.ultrastacks;

import net.neoforged.fml.loading.FMLPaths;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 诊断日志的独立落盘通道。
 *
 * <p>游戏运行期间，Minecraft 自己的 latest.log / debug.log 走 log4j 缓冲，
 * 客户端不退出时文件内容常常不刷新，线上排查根本读不到。这个类把关键诊断行
 * 追加写到 {@code config/ultrastacks-diag.log}，每条写完立即 {@code flush()}，
 * 游戏不退出也能实时读到。
 *
 * <p>Used by the merge/slot diagnostics so evidence lands on disk immediately
 * even while the game is still running (log4j buffers latest.log until exit).
 */
public final class DiagLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final AtomicBoolean INIT = new AtomicBoolean(false);
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
     * 追加一行并立即落盘。写失败时静默（诊断不能干扰游戏逻辑）。
     *
     * <p>Appends one line and flushes immediately; failures are silent.
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
}
