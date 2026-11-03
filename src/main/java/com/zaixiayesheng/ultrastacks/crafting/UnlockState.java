package com.zaixiayesheng.ultrastacks.crafting;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

/**
 * 谁按着解锁键。按键状态只在客户端有，服务端要判断就得靠客户端报上来。
 * 纯内存表，退出即清，跟存档没关系。
     *
 * Who is holding the unlock key. The state only exists on the client, so the server has to be told.
 * In-memory only — nothing is saved.
 */
public final class UnlockState {

    private static final Set<UUID> UNLOCKED = ConcurrentHashMap.newKeySet();

    private UnlockState() {
    }

    /** 记一笔：这人现在按没按住。 */
    public static void set(UUID playerId, boolean unlocked) {
        if (unlocked) {
            UNLOCKED.add(playerId);
        } else {
            UNLOCKED.remove(playerId);
        }
    }

    /** 他按着解锁键就 true。 */
    public static boolean isUnlocked(Player player) {
        return player != null && UNLOCKED.contains(player.getUUID());
    }

    /** 退出时清掉。 */
    public static void clear(UUID playerId) {
        UNLOCKED.remove(playerId);
    }

    /**
     * 当前线程正在服务哪个玩家。{@code getRepeatedQuickMoveLimit} 这类回调拿不到玩家参数，
     * 由外层带玩家的入口进/出，服务端据此判断“这个人按没按住解锁键”。
     *
     * <p>Which player the current thread is serving. Callbacks such as
     * {@code getRepeatedQuickMoveLimit} get no player argument, so the outer call that does have one
     * brackets the region and the server reads the flag from there.
     */
    private static final ThreadLocal<Player> CURRENT = new ThreadLocal<>();

    /** 进入一个带玩家的调用。 / Enter a call that knows the player. */
    public static void enter(Player player) {
        CURRENT.set(player);
    }

    /** 离开。 / Leave it. */
    public static void exit() {
        CURRENT.remove();
    }

    /** 当前线程的玩家按着解锁键就 true；服务端安全（不碰客户端类）。 */
    public static boolean currentUnlocked() {
        Player player = CURRENT.get();
        return player != null && isUnlocked(player);
    }

}
