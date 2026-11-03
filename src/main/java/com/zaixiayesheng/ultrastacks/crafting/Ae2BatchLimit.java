package com.zaixiayesheng.ultrastacks.crafting;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.entity.player.Player;

/**
 * AE 取料入口的停止闸：每做一份配方先记账，超出批量上限就记下“这一轮该停”，
 * 随后的取料返回空，AE 会当成材料不足自然结束。
 *
 * <p>Stop gate for the AE ingredient-extraction step: each recipe run is counted first, and when the
 * cap is exceeded the next extraction returns empty, which AE handles as "out of materials".
 */
public final class Ae2BatchLimit {
    private static final ThreadLocal<Boolean> OVER = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private Ae2BatchLimit() {
    }

    public static void countRun(Player player) {
        OVER.set(!BatchCounter.allow(player.getUUID(), UltraStacksConfig.bulkCraftUnlockKey()
            && UnlockState.isUnlocked(player)));
    }

    public static boolean exhausted() {
        return OVER.get();
    }
}
