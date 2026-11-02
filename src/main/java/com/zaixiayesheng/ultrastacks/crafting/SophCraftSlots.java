package com.zaixiayesheng.ultrastacks.crafting;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * 精妙背包“合成升级”页里的九个合成格。
 *
 * <p>The crafting grid of a Sophisticated backpack lives in an upgrade container that is not a
 * subclass of {@code Container}, so matching by slot class name never finds it. The container map
 * is a field on the menu, and each upgrade container exposes its recipe slots, so we read them by
 * reflection instead of guessing from class names.
 */
public final class SophCraftSlots {

    private static final String CRAFTING = "upgrades.crafting.CraftingUpgradeContainer";

    private SophCraftSlots() {
    }

    /** 返回合成页的九个格子；找不到时返回空表。 */
    @SuppressWarnings("unchecked")
    public static List<Slot> slots(AbstractContainerMenu menu) {
        Object container = craftingContainer(menu);
        if (container == null) {
            return Collections.emptyList();
        }
        Object slots = call(container, "getRecipeSlots");
        if (slots instanceof List<?> list) {
            List<Slot> out = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Slot slot) {
                    out.add(slot);
                }
            }
            return out;
        }
        return Collections.emptyList();
    }


    /** 精妙页签上那个开关：true = 成品/清空进背包存储，false = 进玩家物品栏。 */
    public static boolean intoStorage(AbstractContainerMenu menu) {
        Object container = craftingContainer(menu);
        if (container == null) {
            return true;
        }
        Object value = call(container, "shouldShiftClickIntoStorage");
        return !(value instanceof Boolean b) || b;
    }

    private static Object craftingContainer(AbstractContainerMenu menu) {
        Object map = field(menu, "upgradeContainers");
        if (!(map instanceof Map<?, ?> containers)) {
            return null;
        }
        for (Object value : containers.values()) {
            if (value != null && value.getClass().getName().contains(CRAFTING)) {
                return value;
            }
        }
        return null;
    }

    private static Object field(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        return null;
    }

    private static Object call(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException e) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        return null;
    }
}
