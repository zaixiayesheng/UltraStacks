package com.zaixiayesheng.ultrastacks.display;

/**
 * 数量显示归谁的纯判定，不碰任何游戏类，方便脱离客户端逐条核对。
 *
 * <p>规则只有三条：
 *
 * <ol>
 *   <li>只有 AE 终端、精妙背包 / 存储、精致存储这几个界面自己画数量；其余界面一律归我们。</li>
 *   <li>在那几个界面里，玩家物品栏（容器是 {@code Inventory}）也归我们，否则开着终端看背包就没数字了。</li>
 *   <li>手上拿着的那堆不占槽位，永远归我们；认不出归属的临时展示栈让位给界面自己。</li>
 * </ol>
 *
 * <p>Pure ownership rules for count rendering, with no game classes involved so the truth table can
 * be checked outside the client. Only AE terminals, Sophisticated storage/backpacks and Refined
 * Storage draw their own counts; player-inventory slots stay ours inside those screens, the carried
 * stack is always ours, and unmatched temporary display stacks yield.
 */
public final class DisplayOwnership {

    /** 这些包下的界面自己画数量。 / Screens under these packages draw their own counts. */
    private static final String[] SELF_DRAWING_PACKAGES = {
            "appeng.",
            "de.mari_023.ae2wtlib.",
            "net.p3pp3rf1y.",
            "com.refinedmods.",
            "com.glodblock.github.extendedae."
    };

    private DisplayOwnership() {
    }

    /** 该界面是否自带数量显示。 / Whether this screen draws counts itself. */
    public static boolean screenDrawsOwnCounts(String screenClassName) {
        if (screenClassName == null) {
            return false;
        }
        for (String prefix : SELF_DRAWING_PACKAGES) {
            if (screenClassName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** 精妙核心用来画「数量：x / y」那一行的槽位类。 / The slot class Sophisticated uses for its own count line. */
    public static final String SOPHISTICATED_STORAGE_SLOT =
            "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot";

    /**
     * 精妙会不会自己补提示框那一行数量。
     *
     * <p>精妙核心 {@code StorageScreenBase.getTooltipFromContainerItem} 的条件是：悬停槽位是
     * {@code StorageInventorySlot}，且槽位自己的上限与物品上限不相等。相等时它不画，那一行就得我们来补，
     * 否则提示框里一个数都没有。装了解锁槽位上限的升级后两个数会被拉开，这时候由它自己说。
     *
     * <p>Whether Sophisticated will add its own count line. Its condition is "hovered slot is a
     * {@code StorageInventorySlot} and the slot limit differs from the item limit"; when the two are
     * equal it adds nothing, so the line has to come from us.
     *
     * @param slotClassName 悬停槽位的类名 / class name of the hovered slot
     * @param slotLimit     槽位上限 / slot limit
     * @param itemLimit     物品上限 / item limit
     */
    public static boolean sophisticatedDrawsOwnCount(String slotClassName, int slotLimit, int itemLimit) {
        return SOPHISTICATED_STORAGE_SLOT.equals(slotClassName) && slotLimit != itemLimit;
    }

    /**
     * 这一格的数字是不是该由我们画。
     *
     * @param screenDrawsOwnCounts 界面自带数量显示（{@link #screenDrawsOwnCounts(String)}）
     * @param carried              就是手上拿着的那堆
     * @param foundInSlots         能在某个槽位里找到这堆（按引用）
     * @param playerInventorySlot  找到的那个槽位属于玩家物品栏
     * @return {@code true} 表示归我们，{@code false} 表示让位
     */
    public static boolean ours(boolean screenDrawsOwnCounts, boolean carried,
                               boolean foundInSlots, boolean playerInventorySlot) {
        if (!screenDrawsOwnCounts) {
            return true;
        }
        if (carried) {
            return true;
        }
        if (!foundInSlots) {
            return false;
        }
        return playerInventorySlot;
    }
}
