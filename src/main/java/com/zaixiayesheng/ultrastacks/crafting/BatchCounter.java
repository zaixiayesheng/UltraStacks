package com.zaixiayesheng.ultrastacks.crafting;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.client.UnlockKey;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「一次操作最多做多少份」的计数：同一个人连续合成时累计，停下 250 毫秒就重新开始。
 * 精妙与 AE 都是“一次 Shift 触发一串重复合成”，没有统一的开始/结束回调，所以用空闲间隔切分。
 *
 * <p>Counts how many recipe runs one player has done in a row. Both Sophisticated and AE fire a
 * burst of repeated crafts from a single Shift, with no shared begin/end callback, so the burst is
 * delimited by an idle gap of 250 ms.
 */
public final class BatchCounter {
    private static final long IDLE_NANOS = 250_000_000L;
    private static final Map<UUID, long[]> STATE = new ConcurrentHashMap<>();

    private BatchCounter() {
    }

    /** 只读：当前这一串操作是否已经到上限。 / Read-only: whether the current burst already reached the cap. */
    public static boolean atLimit(UUID player) {
        if (!UltraStacksConfig.craftingModule() || !UltraStacksConfig.bulkCrafting()) {
            return false;
        }
        if (UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld()) {
            return false;
        }
        long[] state = STATE.get(player);
        if (state == null) {
            return false;
        }
        if (System.nanoTime() - state[0] > IDLE_NANOS) {
            return false;
        }
        return state[1] >= Math.max(1, UltraStacksConfig.bulkCraftLimit());
    }

    /** 只读：这一串已经记了多少份（空闲超时就当 0）。 / Read-only: how many runs were counted in this burst. */
    public static long count(UUID player) {
        long[] state = STATE.get(player);
        if (state == null || System.nanoTime() - state[0] > IDLE_NANOS) {
            return 0L;
        }
        return state[1];
    }

    /** 记一份成功的合成。 / Record one successful run. */
    public static void record(UUID player) {
        long now = System.nanoTime();
        long[] state = STATE.computeIfAbsent(player, id -> new long[] {now, 0L});
        if (now - state[0] > IDLE_NANOS) {
            state[1] = 0L;
        }
        state[0] = now;
        state[1]++;
    }

    /** 记一份，返回这一份是否还在上限之内。 / Count one run and report whether it is still allowed. */
    public static boolean allow(UUID player) {
        if (!UltraStacksConfig.craftingModule() || !UltraStacksConfig.bulkCrafting()) {
            return true;
        }
        if (UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld()) {
            return true;
        }
        long now = System.nanoTime();
        long[] state = STATE.computeIfAbsent(player, id -> new long[] {now, 0L});
        if (now - state[0] > IDLE_NANOS) {
            state[1] = 0L;
        }
        state[0] = now;
        state[1]++;
        return state[1] <= Math.max(1, UltraStacksConfig.bulkCraftLimit());
    }
}
