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

}
