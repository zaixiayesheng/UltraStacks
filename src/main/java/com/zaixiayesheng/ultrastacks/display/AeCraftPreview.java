package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.client.UnlockKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * 合成终端产物格的数量预览。
 *
 * <p>Only draws a number; the actual crafting still goes through AE's own slot logic. The craft count is
 * limited by what the client-side ME repo knows about each ingredient, so it matches what a shift-click
 * on the result slot could actually produce right now.
 *
 * <p>26.1.2 没有可编译的 AE2：这个工程不依赖 AE2 的 jar，所以这里全部走「类名字符串 + 反射」，
 * 不做任何 {@code appeng.*} 的编译期引用。运行时 AE2 不在，{@link #isAeMenu} 恒为 false，整条路径自动关闭。
 *
 * <p>26.1.2 ships no AE2 jar to compile against, so every AE type is reached by class-name string and
 * reflection. When AE2 is absent at runtime {@link #isAeMenu} is always false and the whole path is off.
 */
public final class AeCraftPreview {

    private AeCraftPreview() {
    }

    /** AE2 合成终端的菜单类名。 / Menu class of AE2's crafting terminal. */
    private static final String ME_STORAGE_MENU = "appeng.menu.me.common.MEStorageMenu";
    /** 产物槽类名。 / Result slot class. */
    private static final String CRAFTING_TERM_SLOT = "appeng.menu.slot.CraftingTermSlot";
    /** 3×3 合成格槽类名。 / Slot class of the 3×3 crafting grid. */
    private static final String CRAFTING_MATRIX_SLOT = "appeng.menu.slot.CraftingMatrixSlot";

    /** 上一帧算出的批次合计量，供提示框复用（-1 表示这帧没算出来）。 */
    private static volatile long lastTotal = -1L;
    private static volatile net.minecraft.world.item.ItemStack lastResult = net.minecraft.world.item.ItemStack.EMPTY;

    /** 这个菜单是不是 AE2 的合成终端（按类名判断，AE2 不在时恒 false）。 */
    public static boolean isAeMenu(AbstractContainerMenu menu) {
        return menu != null && hasTypeNamed(menu.getClass(), ME_STORAGE_MENU);
    }

    /** 只在悬停的正是合成产物时返回合计量，避免合计行跑到别的物品上。 */
    public static long previewTotal(net.minecraft.world.inventory.Slot slot) {
        if (slot == null || !CRAFTING_TERM_SLOT.equals(slot.getClass().getName())) {
            return -1L;
        }
        net.minecraft.world.item.ItemStack hovered = slot.getItem();
        long total = lastTotal;
        if (total <= 0 || hovered.isEmpty() || lastResult.isEmpty()) {
            return -1L;
        }
        return net.minecraft.world.item.ItemStack.isSameItemSameComponents(lastResult, hovered) ? total : -1L;
    }

    public static void draw(net.minecraft.client.gui.GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen) {
        // 每帧先清掉上一帧的结果：合成格被清空/换配方时，不能再把旧数字挂到别的物品上。
        // Reset the previous frame first: once the grid is cleared the old number must not stick.
        lastTotal = -1L;
        lastResult = net.minecraft.world.item.ItemStack.EMPTY;
        if (!UltraStacksConfig.displayModule()) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        if (!isAeMenu(menu)) {
            return;
        }
        Slot result = null;
        List<ItemStack> grid = new ArrayList<>(9);
        for (Slot slot : menu.slots) {
            if (hasTypeNamed(slot.getClass(), CRAFTING_TERM_SLOT)) {
                result = slot;
            } else if (hasTypeNamed(slot.getClass(), CRAFTING_MATRIX_SLOT)) {
                grid.add(slot.getItem());
            }
        }
        if (result == null || grid.size() != 9) {
            return;
        }
        ItemStack out = result.getItem();
        if (out.isEmpty()) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        var manager = ResultPreview.recipeManager();
        if (manager == null) {
            return;
        }
        var holder = manager
            .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), level)
            .orElse(null);
        if (holder == null) {
            return;
        }
        Object repo = clientRepo(menu);
        long crafts = ResultPreview.groupedCrafts(holder.value().placementInfo().ingredients(), ingredient -> {
            long have = storedFor(repo, ingredient);
            for (ItemStack stack : grid) {
                if (ingredient.test(stack)) {
                    have += stack.getCount();
                }
            }
            return have;
        });
        if (crafts <= 0L) {
            return;
        }
        boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld();
        long batch = unlocked ? crafts
            : (shiftHeld() ? Math.min(UltraStacksConfig.bulkCraftLimit(), crafts) : 1L);
        long total = out.getCount() * batch;
        lastTotal = total > out.getCount() ? total : -1L;
        lastResult = out.copyWithCount(1);
        if (total <= out.getCount()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int[] origin = ResultPreview.origin(graphics, screen);
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(0.0F, 0.0F);
        CountRenderer.drawForSlot(graphics, font, CountFormat.format(total), result.x + origin[0],
            result.y + origin[1]);
        pose.popMatrix();
        ResultPreview.rememberCell(result.x + origin[0], result.y + origin[1], total);
    }

    /** 26.1.2 删了 {@code Screen#hasShiftDown}，改问 GLFW。 / Shift state asked from GLFW. */
    private static boolean shiftHeld() {
        var window = Minecraft.getInstance().getWindow();
        if (window == null) {
            return false;
        }
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
            || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /** {@code menu.getClientRepo()}，反射取，拿不到就 null。 */
    private static Object clientRepo(AbstractContainerMenu menu) {
        try {
            var method = menu.getClass().getMethod("getClientRepo");
            return method.invoke(menu);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 仓库里某配方的存量：{@code repo.getByIngredient(...)} 各项 {@code getStoredAmount()} 求和。 */
    private static long storedFor(Object repo, Ingredient ingredient) {
        if (repo == null) {
            return 0L;
        }
        try {
            var method = repo.getClass().getMethod("getByIngredient", Ingredient.class);
            if (!(method.invoke(repo, ingredient) instanceof Iterable<?> entries)) {
                return 0L;
            }
            long have = 0L;
            for (Object entry : entries) {
                have += storedAmount(entry);
            }
            return have;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    /** 单个仓库条目的存量。 / Stored amount of one repository entry. */
    private static long storedAmount(Object entry) {
        if (entry == null) {
            return 0L;
        }
        try {
            var method = entry.getClass().getMethod("getStoredAmount");
            return method.invoke(entry) instanceof Number n ? n.longValue() : 0L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    /** 类自身或其任一父类/接口是否叫这个名字（AE2 不在时不会抛）。 */
    private static boolean hasTypeNamed(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (c.getName().equals(name)) {
                return true;
            }
            for (Class<?> itf : c.getInterfaces()) {
                if (itf.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
