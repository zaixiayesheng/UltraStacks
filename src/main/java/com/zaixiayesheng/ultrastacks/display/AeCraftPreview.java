package com.zaixiayesheng.ultrastacks.display;

import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.slot.CraftingMatrixSlot;
import appeng.menu.slot.CraftingTermSlot;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.client.UnlockKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
 */
public final class AeCraftPreview {

    private AeCraftPreview() {
    }

    /** 上一帧算出的批次合计量，供提示框复用（-1 表示这帧没算出来）。 */
    private static volatile long lastTotal = -1L;
    private static volatile net.minecraft.world.item.ItemStack lastResult = net.minecraft.world.item.ItemStack.EMPTY;

    /** 只在悬停的正是合成产物时返回合计量，避免合计行跑到别的物品上。 */
    public static long previewTotal(net.minecraft.world.inventory.Slot slot) {
        if (slot == null || !"appeng.menu.slot.CraftingTermSlot".equals(slot.getClass().getName())) {
            return -1L;
        }
        net.minecraft.world.item.ItemStack hovered = slot.getItem();
        long total = lastTotal;
        if (total <= 0 || hovered.isEmpty() || lastResult.isEmpty()) {
            return -1L;
        }
        return net.minecraft.world.item.ItemStack.isSameItemSameComponents(lastResult, hovered) ? total : -1L;
    }

    public static void draw(net.minecraft.client.gui.GuiGraphics graphics, AbstractContainerScreen<?> screen) {
        // 每帧先清掉上一帧的结果：合成格被清空/换配方时，不能再把旧数字挂到别的物品上。
        // Reset the previous frame first: once the grid is cleared the old number must not stick.
        lastTotal = -1L;
        lastResult = net.minecraft.world.item.ItemStack.EMPTY;
        if (!UltraStacksConfig.displayModule() || !(screen.getMenu() instanceof MEStorageMenu menu)) {
            return;
        }
        Slot result = null;
        List<ItemStack> grid = new ArrayList<>(9);
        for (Slot slot : menu.slots) {
            if (slot instanceof CraftingTermSlot) {
                result = slot;
            } else if (slot instanceof CraftingMatrixSlot) {
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
        var holder = level.getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), level)
            .orElse(null);
        if (holder == null) {
            return;
        }
        var repo = menu.getClientRepo();
        long crafts = ResultPreview.groupedCrafts(holder.value().getIngredients(), ingredient -> {
            long have = 0L;
            if (repo != null) {
                for (GridInventoryEntry entry : repo.getByIngredient(ingredient)) {
                    have += entry.getStoredAmount();
                }
            }
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
            : (Screen.hasShiftDown() ? Math.min(UltraStacksConfig.bulkCraftLimit(), crafts) : 1L);
        long total = out.getCount() * batch;
        lastTotal = total > out.getCount() ? total : -1L;
        lastResult = out.copyWithCount(1);
        if (total <= out.getCount()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int[] origin = ResultPreview.origin(graphics, screen);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 300.0F);
        CountRenderer.drawForSlot(graphics, font, CountFormat.format(total), result.x + origin[0],
            result.y + origin[1]);
        pose.popPose();
        ResultPreview.rememberCell(result.x + origin[0], result.y + origin[1], total);
    }
}
