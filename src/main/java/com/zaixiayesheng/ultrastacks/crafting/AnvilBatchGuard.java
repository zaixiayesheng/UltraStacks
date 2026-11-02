package com.zaixiayesheng.ultrastacks.crafting;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

/**
 * 每次点击最多做多少件。
 *
 * <p>Caps how many anvil operations a single click may perform. Our own shift-click loop already
 * stops at the configured batch size, but other menus (the Sophisticated Backpacks anvil tab runs
 * its own take path) can keep going; counting takes per tick and refusing to produce another result
 * once the cap is reached keeps every path on the same budget.
 */
public final class AnvilBatchGuard {

    /** 一个玩家在同一 tick 里已经取走的次数。 / Takes already performed this tick. */
    private static final Map<UUID, long[]> STATE = new ConcurrentHashMap<>();

    /**
     * 我们自己那圈 shift 循环里不再收产物：循环自己按额度停，停下时产物该留着给玩家看。
     *
     * Inside our own shift loop the result must stay visible — the loop already stops at the cap,
     * so clearing the slot there would only produce a red X the player has to fix by re-placing.
     */
    private static final ThreadLocal<Boolean> IN_LOOP = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private AnvilBatchGuard() {
    }

    /** 记一次取走。 / Records one take. */
    public static void noteTake(Player player) {
        if (player == null || player.level() == null) {
            return;
        }
        long tick = player.level().getGameTime();
        long[] entry = STATE.computeIfAbsent(player.getUUID(), id -> new long[] { tick, 0 });
        synchronized (entry) {
            if (entry[0] != tick) {
                entry[0] = tick;
                entry[1] = 0;
            }
            entry[1]++;
        }
    }

    /** 本 tick 已经取走几次。 / How many takes happened this tick. */
    public static int takesThisTick(Player player) {
        if (player == null || player.level() == null) {
            return 0;
        }
        long[] entry = STATE.get(player.getUUID());
        if (entry == null) {
            return 0;
        }
        synchronized (entry) {
            return entry[0] == player.level().getGameTime() ? (int) entry[1] : 0;
        }
    }

    /** 这一 tick 的额度用完了没有。 / Whether this tick's budget is spent. */
    public static void enterLoop() {
        IN_LOOP.set(Boolean.TRUE);
    }

    public static void leaveLoop() {
        IN_LOOP.set(Boolean.FALSE);
    }

    public static boolean overLimit(Player player) {
        if (Boolean.TRUE.equals(IN_LOOP.get())) {
            return false;
        }
        boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey() && UnlockState.isUnlocked(player);
        // 按住解锁键就不设上限，做空左槽为止。
        // Holding the unlock key leaves no cap: the batch runs until the left slot is empty.
        int cap = unlocked ? Integer.MAX_VALUE : Math.max(1, UltraStacksConfig.bulkCraftLimit());
        return takesThisTick(player) >= cap;
    }

    /** 玩家下线时清掉。 / Forgets a player on logout. */
    public static void clear(UUID id) {
        STATE.remove(id);
    }
}
