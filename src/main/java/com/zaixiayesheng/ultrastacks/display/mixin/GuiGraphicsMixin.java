package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.display.CountFormat;
import com.zaixiayesheng.ultrastacks.display.CountRenderer;
import com.zaixiayesheng.ultrastacks.display.DisplayOwnership;
import com.zaixiayesheng.ultrastacks.display.ResultPreview;
import com.zaixiayesheng.ultrastacks.display.SlotLookup;
import com.zaixiayesheng.ultrastacks.display.SlotRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 接管物品栏与容器里那串计数文本的绘制。
 *
 * <p>计数不超过 99、显示模块关掉、调用方自己给了 {@code customText}，或者原版传进来的是 {@code null}，都照原样画。
 * 超过 99 时把数字换成 {@link CountFormat#format(long)} 的结果——默认 1000 以下仍是完整数字，之上缩写成
 * k / m / b，阈值由 {@code abbreviateAbove} 控制。
 *
 * <p>缩写后字宽变窄，所以交给 {@link CountRenderer} 按槽位坐标右对齐重画，字号与精妙自己的数量保持一致。
 *
 * <p>Takes over the drawing of the count text in inventories and containers. A count of at most 99, a
 * disabled display module, a caller-supplied {@code customText} or a {@code null} text from vanilla all
 * fall through to the original drawing. Above 99 the digits are replaced by
 * {@link CountFormat#format(long)} and redrawn through {@link CountRenderer} so the size matches
 * Sophisticated's own numbers.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    /** 槽位留给数量的横向像素。 / Horizontal pixels a slot leaves for the count. */
    private static final int SLOT_TEXT_WIDTH = 16;

    /**
     * 判断这一格的数量该不该由别人显示。
     *
     * <p>只有两类界面会抢数量显示：AE 的终端、精妙的背包 / 存储、精致存储的网格。它们在自己的网格上画缩写数量，
     * 我们再叠一层就重复了。除此之外的界面一律由我们画。
     *
     * <p>在这三类界面里也只让出网格那部分：玩家物品栏（容器是
     * {@code net.minecraft.world.entity.player.Inventory}）仍然归我们。
     *
     * <p>Only a few screens take over count rendering: AE terminals, Sophisticated backpacks/storage and
     * Refined Storage grids. Inside those screens only the grid yields; player-inventory slots remain
     * ours so an open terminal still shows counts for your own inventory.
     */
    private static boolean ultrastacks$isForeignGridSlot(ItemStack stack, int itemX, int itemY) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return false;
        }
        String name = screen.getClass().getName();
        if (!DisplayOwnership.screenDrawsOwnCounts(name)) {
            return false;
        }
        // AE 的合成格自己画原始数字（不缩写），这一格交给我们，让 7281 变成 7.2k。
        if (ultrastacks$isCraftingMatrixSlot(container, itemX, itemY)) {
            return false;
        }
        boolean carried = container.getMenu().getCarried() == stack;
        // 优先用「正在画的槽位」：原版逐格绘制时已经记下了，连查表都不用。
        Slot drawing = SlotRenderContext.current;
        boolean found;
        boolean playerSideSlot;
        if (drawing != null) {
            found = true;
            String drawingClass = drawing.getClass().getName();
            playerSideSlot = drawing.container instanceof net.minecraft.world.entity.player.Inventory
                    || drawingClass.contains("ArmorSlot")
                    || drawingClass.contains("OffhandSlot");
        } else {
            SlotLookup.Info info = SlotLookup.find(container.getMenu(), itemX, itemY);
            found = info.found();
            playerSideSlot = info.playerSide();
        }
        boolean aeScreen = name.startsWith("appeng.") || name.startsWith("de.mari_023.ae2wtlib.")
                || name.startsWith("com.glodblock.github.extendedae.");
        if (aeScreen) {
            // AE 系界面里只画「玩家自己身上」的格子，其余一律不画，免得两位数字压在一起。
            return !(playerSideSlot || carried);
        }
        if (playerSideSlot) {
            return false;
        }
        // 坐标对不上任何槽位的是 HUD 快捷栏或界面外绘制，不该让位。
        return found && !carried && !playerSideSlot;
    }

    /**
     * 这一格是不是 AE 自己画的（存储列表之类不是真槽位的地方）。
     * 认不出槽位的 AE 界面格子归它自己，只有真槽（接口配置格、物品栏）才由我们补数字。
     *
     * <p>Whether AE paints this cell itself: cells that match no slot in an AE screen belong to AE's own
     * rendering, only real slots are ours.
     */
    private static boolean ultrastacks$aeSelfDrawnCell(int itemX, int itemY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null || minecraft.player == null) {
            return false;
        }
        String screenName = minecraft.screen.getClass().getName();
        boolean aeScreen = screenName.startsWith("appeng.") || screenName.startsWith("de.mari_023.ae2wtlib.")
                || screenName.startsWith("com.glodblock.github.extendedae.");
        if (!aeScreen) {
            return false;
        }
        return !SlotLookup.find(minecraft.player.containerMenu, itemX, itemY).found();
    }

    private static boolean ultrastacks$isCraftingMatrixSlot(AbstractContainerScreen<?> container, int itemX,
            int itemY) {
        return "appeng.menu.slot.CraftingMatrixSlot".equals(
                SlotLookup.find(container.getMenu(), itemX, itemY).slotClass());
    }


    /** 当前界面是否属于「自己画数量」的那几家（精妙、精致存储）。 / Screens that draw their own counts. */
    private static boolean ultrastacks$drawsOwnCounts() {
        var screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (screen == null) {
            return false;
        }
        String name = screen.getClass().getName();
        return name.startsWith("net.p3pp3rf1y.") || name.startsWith("com.refinedmods.");
    }

    private static int shownWidth(String text, Font font) {
        return font.width(text);
    }

    /**
     * 这一格是不是已经由我们的批次预览接管了。
     *
     * <p>先看渲染上下文给的槽位，拿不到（精妙自己的 renderSlot 不给上下文）就按坐标在菜单里找。
     */
    private static boolean ultrastacks$anvilResultSilent(int itemX, int itemY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }
        if (!(screen.getMenu() instanceof AnvilMenu)) {
            return false;
        }
        Slot slot = SlotRenderContext.current;
        if (slot == null || slot.index != 2) {
            slot = null;
            for (Slot candidate : screen.getMenu().slots) {
                if (candidate.x == itemX && candidate.y == itemY) {
                    slot = candidate;
                    break;
                }
            }
        }
        if (slot == null || slot.index != 2 || slot.getItem().isEmpty()) {
            return false;
        }
        return ResultPreview.previewTotal(screen, slot) <= slot.getItem().getCount();
    }

    private static boolean ultrastacks$previewOwnsSlot(int itemX, int itemY) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return false;
        }
        Slot slot = SlotRenderContext.current;
        if (slot == null) {
            for (Slot candidate : container.getMenu().slots) {
                if (candidate.x == itemX && candidate.y == itemY) {
                    slot = candidate;
                    break;
                }
            }
        }
        if (slot == null || slot.getItem().isEmpty()) {
            // 精妙自己的槽位不在菜单里，退回到「预览这一帧画过这格」的记录。
            return ResultPreview.ownsCell(itemX, itemY);
        }
        // 只有真正的产物格才归预览：别的格子哪怕放着同名物品，也不能把它的真实数量吞掉。
        // Only the real result slot belongs to the preview: a cell that merely holds the same item
        // must keep its own count.
        if (!ultrastacks$isResultSlot(slot)) {
            return false;
        }
        long previewTotal = ResultPreview.previewTotal(container, slot);
        // 只有「预览画出的数字」和槽位里的真实数量不一样时，才由我们接管这一格。
        return previewTotal > 0L;
    }

    /** 是不是合成产物格（AE 的合成产物槽、原版/精妙的第 3 个容器槽）。 */
    private static boolean ultrastacks$isResultSlot(Slot slot) {
        net.minecraft.client.gui.screens.Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen) {
            return com.zaixiayesheng.ultrastacks.display.ResultPreview.isResultSlotFor(containerScreen, slot);
        }
        return false;
    }

    @Redirect(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
            )
    )
    private int ultrastacks$compactCount(GuiGraphics graphics, Font font, String text, int x, int y, int color,
            boolean dropShadow,
            Font itemFont, ItemStack stack, int itemX, int itemY,
            @Nullable String customText) {
        // 产物格上我们要画「一批能做多少」，原版那串「一次的量」就别再画了。
        if (UltraStacksConfig.displayModule() && ultrastacks$anvilResultSilent(itemX, itemY)) {
            return 0;
        }
        if (UltraStacksConfig.displayModule() && ultrastacks$previewOwnsSlot(itemX, itemY)) {
            return 0;
        }
        // AE 用数量 0 的"幽灵栈"占位，原版只判 count != 1 就会把这个 0 画出来。
        // AE parks cells with a zero-count ghost stack; vanilla only skips count == 1, so it paints a "0".
        if (stack.getCount() <= 0) {
            return 0;
        }
        if (!UltraStacksConfig.displayModule() || text == null || stack.getCount() <= 99) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }
        // 传了非空文本 = 调用方自己排好了字，照它的画。传空串是两种意思：精妙表示「我自己画这格」，
        // AE 表示「我不想画」——所以 AE 的空串接过来画缩写，精妙的空串继续让开。
        boolean rawCountText = customText != null && customText.equals(Integer.toString(stack.getCount()));
        if (customText != null && !customText.isEmpty() && !rawCountText) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }
        if (customText != null && customText.isEmpty()) {
            // 空串有两种来源：精妙表示「这格我自己画」，AE 表示「我不画数字」。
            // 精妙继续让开，AE 由我们补上缩写 —— 否则玩家物品栏里的大堆会一格数字都没有。
            // An empty string means "this cell is mine" in Sophisticated and "draw no number" in AE:
            // yield to Sophisticated, but fill in our abbreviation for AE so big stacks keep a count.
            if (ultrastacks$drawsOwnCounts()) {
                return 0;
            }
        }
        if (customText != null && ultrastacks$skipForeignCount(stack, itemX, itemY)) {
            return 0;
        }
        // AE 的存储列表不是真槽位，数字由它自己那层画；再画一次会叠成两层。
        // AE's storage list is not made of real slots and draws its own numbers; a second pass doubles them.
        if (customText != null && ultrastacks$aeSelfDrawnCell(itemX, itemY)) {
            return 0;
        }
        if (customText != null && ultrastacks$isForeignGridSlot(stack, itemX, itemY)) {
            // 让位时如果原版文本本来就是空的（AE 常传空串），让给谁都没人画，于是整格空白。
            // 这种情况由我们补上缩写，避免大堆看起来只有 1 个。
            // Yielding is pointless when the vanilla text is already empty: nobody draws and the
            // cell ends up blank. Fill in our abbreviation instead.
            if (!text.isEmpty()) {
                return graphics.drawString(font, text, x, y, color, dropShadow);
            }
            String fallback = CountFormat.format(stack.getCount());
            CountRenderer.drawForSlot(graphics, font, fallback, itemX, itemY);
            return shownWidth(fallback, font);
        }

        String shown = CountFormat.format(stack.getCount());
        if (shown.isEmpty()) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }
        // 坐标、缩放、颜色全部照抄精妙的 renderStackCount，保证同一排里两边字号一致。
        CountRenderer.drawForSlot(graphics, font, shown, itemX, itemY);
        return shownWidth(text, font);
    }

    /**
     * 这一格的数量是不是干脆不该由任何人画（交给界面自己）。
     *
     * <p>只有 AE 系界面会这样：它的 ME 列表、合成矩阵自己画数字。玩家侧的格子除外。
     */
    private static boolean ultrastacks$skipForeignCount(ItemStack stack, int itemX, int itemY) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return false;
        }
        String name = screen.getClass().getName();
        boolean aeScreen = name.startsWith("appeng.") || name.startsWith("de.mari_023.ae2wtlib.")
                || name.startsWith("com.glodblock.github.extendedae.");
        if (!aeScreen) {
            return false;
        }
        if (container.getMenu().getCarried() == stack) {
            return false;
        }
        SlotLookup.Info info = SlotLookup.find(container.getMenu(), itemX, itemY);
        if ("appeng.menu.slot.CraftingMatrixSlot".equals(info.slotClass())) {
            return false;
        }
        if (info.found()) {
            return !info.playerSide();
        }
        // 认不出槽位的格子（ExtendedAE 综合终端那类自绘网格）也一律不画。
        return true;
    }
}
