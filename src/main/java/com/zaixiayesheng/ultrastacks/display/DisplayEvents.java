package com.zaixiayesheng.ultrastacks.display;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 显示模块的另一半：物品提示框。
 *
 * <p>槽位里的数量会被缩写成 k / m / b，提示框补上准确数量与当前堆叠上限，信息不会丢。只在显示模块
 * 与提示框开关都打开、且数量超过 99 时添加，普通物品的提示框保持原样。配色与精妙自己那一行一致，
 * 装没装解锁槽位上限的升级都不会跳风格。
 *
 * <p>The other half of the display module: the item tooltip. Slot counts are abbreviated to
 * k / m / b, so the tooltip supplies the exact count together with the current stack limit and no
 * information is lost. The line is only added when both the display module and the tooltip switch
 * are on and the count is above 99, which leaves ordinary item tooltips untouched.
 */
@Mod.EventBusSubscriber(modid = UltraStacks.MOD_ID, value = Dist.CLIENT)
public final class DisplayEvents {
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();

    /**
     * 上一次记下的判定，避免工具提示每帧刷新时刷屏。
     *
     * <p>Last logged decision, so tooltip redraws do not flood the log.
     */
    private static String lastDecision;

    private DisplayEvents() {
    }

    /**
     * 把「数量：x / y」接到提示框最末尾。
     *
     * <p>走槽位提示框的构建路径（{@code getTooltipFromContainerItem}）而不是物品提示框事件：模组名是在
     * 那个事件之后追加的，在这里接才能让这一行排在模组名下面，和精妙自己那行同一位置。
     *
     * <p>Appends the count line to the end of the tooltip. It hooks the slot-tooltip builder instead
     * of the item tooltip event because the mod name is appended after that event; adding here keeps
     * the line below the mod name, exactly where Sophisticated's own line sits.
     */
    public static List<Component> withCountLine(List<Component> tooltip, ItemStack stack,
                                                AbstractContainerScreen<?> screen) {
        if (!UltraStacksConfig.displayModule() || !UltraStacksConfig.tooltipExact()) {
            return tooltip;
        }
        if (stack.isEmpty()) {
            return tooltip;
        }
        if (hasOurLine(tooltip)) {
            return tooltip;
        }
        Slot hovered = screen == null ? null : screen.getSlotUnderMouse();
        long batchTotal = -1L;
        if (hovered != null && (hovered.getItem() == stack
                || ItemStack.isSameItemSameTags(hovered.getItem(), stack))) {
            batchTotal = ResultPreview.previewTotal(screen, hovered);
        }
        if (batchTotal <= stack.getCount() && screen != null) {
            // 精妙升级页的产物槽不在菜单里，getSlotUnderMouse() 拿不到，按鼠标坐标反查我们画过的格子。
            // The upgrade tab keeps its result slot outside the menu, so the cell is looked up by position.
            long byMouse = ResultPreview.previewTotalUnderMouse(screen);
            if (byMouse > batchTotal) {
                batchTotal = byMouse;
                DiagLog.lineThrottled("tooltip.mouse." + screen.getClass().getSimpleName(),
                        "[diag] tooltip by mouse: screen=" + screen.getClass().getSimpleName()
                                + " hovered=" + (hovered == null ? "null" : hovered.getClass().getSimpleName())
                                + " ours=" + byMouse + " count=" + stack.getCount());
            }
        }
        boolean wantsCount = stack.getCount() > 99 && !sophisticatedSaysItItself(screen, stack);
        if (!wantsCount && batchTotal <= stack.getCount()) {
            return tooltip;
        }
        List<Component> copy = new ArrayList<>(tooltip);
        if (batchTotal > stack.getCount()) {
            // 产物格只放一份，所以把「合计 / 数量」写在一行里，避免两行叠在一起看不清。
            // The result slot holds one craft, so the batch total and the plain count share one line.
            copy.add(batchLine(batchTotal, stack));
        } else if (wantsCount) {
            copy.add(Component.translatable("ultrastacks.tooltip.count",
                    aqua(stack.getCount()), aqua(stack.getMaxStackSize()))
                    .withStyle(ChatFormatting.GRAY));
        }
        return copy;
    }

    /** 已经带有我们这两行之一时不要重复加。 */
    private static boolean hasOurLine(List<Component> tooltip) {
        for (Component line : tooltip) {
            if (line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && (t.getKey().equals("ultrastacks.tooltip.batch")
                        || t.getKey().equals("ultrastacks.tooltip.count"))) {
                return true;
            }
        }
        return false;
    }

    private static Component batchLine(long total, ItemStack stack) {
        return Component.translatable("ultrastacks.tooltip.batch",
                aqua((int) Math.min(Integer.MAX_VALUE, total)), aqua(stack.getMaxStackSize()))
                .withStyle(ChatFormatting.GRAY);
    }

    /**
     * 数量用精妙那一行的配色：数字深青、整行灰色，带千位分隔。
     *
     * <p>同一套配色，装没装解锁槽位上限的升级看起来都一样，切换时不会跳风格。
     *
     * <p>Same colouring as the line Sophisticated draws itself — numbers dark aqua with thousands
     * separators, the rest gray — so the tooltip does not change style when the slot limit is
     * raised or not.
     */
    private static Component aqua(int value) {
        return Component.literal(NUMBER_FORMAT.format(value))
                .withStyle(ChatFormatting.DARK_AQUA);
    }

    /**
     * 精妙会不会自己写那一行数量；会写就让给它，免得两个数并排出现。
     *
     * <p>Whether Sophisticated writes the count line by itself; if so we stay out of the way so the
     * tooltip does not end up with two different numbers.
     */
    private static boolean sophisticatedSaysItItself(AbstractContainerScreen<?> screen, ItemStack stack) {
        Slot hovered = screen == null ? null : screen.getSlotUnderMouse();
        if (hovered == null) {
            return false;
        }
        // 只有鼠标下那格确实放着这个物品，才谈得上「让给它」；否则（比如手里拿着东西停在格子上）
        // 拿别格的上限去比会得出错误结论。
        if (hovered.getItem() != stack) {
            return false;
        }
        String name = null;
        for (Class<?> type = hovered.getClass(); type != null; type = type.getSuperclass()) {
            if (DisplayOwnership.SOPHISTICATED_STORAGE_SLOT.equals(type.getName())) {
                name = type.getName();
                break;
            }
        }
        if (name == null) {
            return false;
        }
        boolean theirs = DisplayOwnership.sophisticatedDrawsOwnCount(name,
                hovered.getMaxStackSize(), stack.getMaxStackSize());
        String decision = name + " slot=" + hovered.getMaxStackSize()
                + " item=" + stack.getMaxStackSize() + " -> " + (theirs ? "sophisticated draws" : "we draw");
        if (!decision.equals(lastDecision)) {
            lastDecision = decision;
            DiagLog.line("tooltip owner: " + decision);
        }
        return theirs;
    }

    /**
     * 悬停产物格时把合计行补进提示框（走事件，覆盖精妙这类自己拼列表的界面）。
     *
     * <p>Adds the batch line through the tooltip event, which also covers screens that build their
     * own tooltip list.
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        net.minecraft.client.gui.screens.Screen current = net.minecraft.client.Minecraft.getInstance().screen;
        if (!(current instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) {
            return;
        }
        // 只追加、不重建列表：创造模式界面的提示框会往固定下标插元素，清空后会越界。
        // Only append — never rebuild the list, because the creative screen inserts at a fixed index.
        if (hasOurLine(event.getToolTip())) {
            return;
        }
        long total = ResultPreview.totalUnderMouse(screen);
        ItemStack stack = ResultPreview.stackUnderMouse();
        if (total <= 0 || stack.isEmpty()) {
            return;
        }
        event.getToolTip().add(batchLine(total, stack));
    }

    /**
     * 鼠标下那格是产物、且我们画过合计时，给整条提示框补一行；已有同类行就不重复加。
     *
     * <p>Appends one batch line when the cursor sits on a result cell we drew, skipping duplicates.
     */
    public static List<Component> withMouseCellLine(List<Component> tooltip) {
        if (!UltraStacksConfig.displayModule() || !UltraStacksConfig.tooltipExact()) {
            return tooltip;
        }
        if (hasOurLine(tooltip)) {
            return tooltip;
        }
        net.minecraft.client.gui.screens.Screen current = net.minecraft.client.Minecraft.getInstance().screen;
        if (!(current instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) {
            return tooltip;
        }
        long total = ResultPreview.totalUnderMouse(screen);
        ItemStack stack = ResultPreview.stackUnderMouse();
        if (total <= 0 || stack.isEmpty()) {
            return tooltip;
        }
        List<Component> copy = new ArrayList<>(tooltip);
        copy.add(batchLine(total, stack));
        return copy;
    }
}
