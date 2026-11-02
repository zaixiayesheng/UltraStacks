package com.zaixiayesheng.ultrastacks.display;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * 槽位坐标查询缓存。
 *
 * <p>渲染每一格数量时都要判断「这格是谁的」，原来每次都遍历整个菜单的槽位表，终端里几百个槽位、
 * 每帧上百格就是把一次 O(槽位数) 的扫描重复上百遍。这里按菜单缓存一份坐标表，菜单换了或槽位
 * 数量变了才重建。
 *
 * <p>Per-frame lookup cache for slot coordinates. Deciding who owns a cell used to walk the whole
 * menu slot list for every drawn cell; with a few hundred slots and a hundred cells per frame that
 * is the same scan repeated a hundred times. The map is rebuilt only when the menu changes.
 */
public final class SlotLookup {

    /** 一个坐标上的槽位信息。 / Slot facts for one position. */
    public record Info(boolean found, boolean playerSide, String slotClass) {
    }

    private static final Info MISSING = new Info(false, false, "");

    private static AbstractContainerMenu menu;
    private static int slotCount = -1;
    private static Map<Long, Info> byPosition = Map.of();

    private SlotLookup() {
    }

    /** 取某个槽位坐标的信息（不用每次扫槽位表）。 / Slot facts at a position, without rescanning. */
    public static Info find(AbstractContainerMenu currentMenu, int x, int y) {
        if (currentMenu == null) {
            return MISSING;
        }
        if (currentMenu != menu || currentMenu.slots.size() != slotCount) {
            rebuild(currentMenu);
        }
        return byPosition.getOrDefault(key(x, y), MISSING);
    }

    /** 菜单或槽位数量变化时重建。 / Rebuild after the menu or its slot count changes. */
    private static void rebuild(AbstractContainerMenu currentMenu) {
        menu = currentMenu;
        slotCount = currentMenu.slots.size();
        Map<Long, Info> built = new HashMap<>(slotCount * 2);
        for (Slot slot : currentMenu.slots) {
            String className = slot.getClass().getName();
            boolean playerSide = slot.container instanceof Inventory
                    || className.contains("ArmorSlot")
                    || className.contains("OffhandSlot");
            built.put(key(slot.x, slot.y), new Info(true, playerSide, className));
        }
        byPosition = built;
    }

    private static long key(int x, int y) {
        return ((long) x << 32) ^ (y & 0xFFFFFFFFL);
    }
}
