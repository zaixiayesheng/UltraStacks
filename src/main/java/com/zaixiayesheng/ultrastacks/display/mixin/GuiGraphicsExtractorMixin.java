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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 接管物品栏与容器里那串计数文本的绘制。
 *
 * <p>26.1.2 把渲染拆成了 render-state 收集：{@code GuiGraphics} 没了，数量由
 * {@code GuiGraphicsExtractor#itemCount} 写进 {@code guiRenderState}。注入点因此落在
 * {@code itemCount} 里那次 {@code text} 调用上，而不是 1.21.1 的
 * {@code renderItemDecorations} / {@code drawString} 组合。
 *
 * <p>这个文件是 1.21.1 的 {@code GuiGraphicsMixin} 与 26.1.2 原有实现的合并版：判断顺序、产物格让位、
 * 精妙 / AE 归属规则都照 1.21.1，只是出口从 {@code drawString} 换成 {@code text}（返回 void，所以
 * 原版那条分支不再需要返回宽度）。
 *
 * <p>计数不超过 99、显示模块关掉、调用方自己给了文本（{@code countText}）都照原样画。超过 99 时把数字
 * 换成 {@link CountFormat#format(long)} 的结果，再交给 {@link CountRenderer} 按同一套几何绘制。
 *
 * <p>Takes over the drawing of the count text. This is 1.21.1's {@code GuiGraphicsMixin} merged into
 * 26.1.2's extractor variant: the decision order, result-slot suppression and the Sophisticated / AE
 * ownership rules are unchanged, only the exit point moved from {@code drawString} to {@code text}
 * (which returns void, so the vanilla branch no longer returns the width).
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {

    /** 槽位留给数量的横向像素。 / Horizontal pixels a slot leaves for the count. */
    private static final int SLOT_TEXT_WIDTH = 16;

    /**
     * 判断这一格的数量该不该由别人显示。
     *
     * <p>只有三类界面会抢数量显示：AE 的终端、精妙的背包 / 存储、精致存储的网格。它们在自己的网格上画
     * 缩写数量，我们再叠一层就重复了。除此之外的界面一律由我们画。
     *
     * <p>在这三类界面里也只让出网格那部分：玩家物品栏（容器是
     * {@code net.minecraft.world.entity.player.Inventory}）仍然归我们。
     *
     * <p>Only a few screens take over count rendering: AE terminals, Sophisticated backpacks/storage and
     * Refined Storage grids. Inside those only the grid yields; player-inventory slots remain ours.
     */
    @SuppressWarnings("unused")
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
        // 优先用「正在画的槽位」：原版逐格绘制时我们已经记下了，连一次查表都不用。
        Slot drawing = SlotRenderContext.current;
        boolean found;
        boolean playerSideSlot;
        if (drawing != null) {
            found = true;
            String drawingClass = drawing.getClass().getName();
            playerSideSlot = drawing.container instanceof Inventory
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
            // AE 系界面里只画「玩家自己身上」的格子，其余一律不画，否则会和 AE 自己的数字叠在一起。
            return !(playerSideSlot || carried);
        }
        if (playerSideSlot) {
            return false;
        }
        // 精致存储只让它自己的资源格（ResourceSlot）自己画；合成格是原版 ResultSlot/CraftingGridSlot，
        // 它自己一笔都不画，让位过去就是整格空白。这类槽我们照常出缩写。
        // Refined Storage may paint its own resource slots, but its crafting slots are plain vanilla
        // ResultSlot/CraftingGridSlot and paint nothing — yielding there leaves the cell blank.
        String drawingName = SlotRenderContext.current != null
                ? SlotRenderContext.current.getClass().getName()
                : SlotLookup.find(container.getMenu(), itemX, itemY).slotClass();
        if (name.startsWith("com.refinedmods.") && !drawingName.startsWith("com.refinedmods.")) {
            return false;
        }
        // 坐标对不上任何槽位的是 HUD 快捷栏，不属于任何槽位，不该让位。
        return found && !carried && !playerSideSlot;
    }

    /**
     * 这一格的数量是不是干脆不该由任何人画（交给界面自己）。
     *
     * <p>Only AE screens skip entirely: their ME listing and crafting grid paint their own counts.
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
        if ("appeng.menu.slot.CraftingMatrixSlot"
                .equals(SlotLookup.find(container.getMenu(), itemX, itemY).slotClass())) {
            return false;
        }
        SlotLookup.Info info = SlotLookup.find(container.getMenu(), itemX, itemY);
        if (info.found()) {
            // ME 接口、存储总线配置这类格子在 AE 里是 FakeSlot/OptionalFakeSlot：AE 只画物品、不画数量，
            // 我们先前把它们当「AE 自己画」整片让位，结果两边都不画，看着像格子是空的。
            // Config slots (ME interface, storage bus) are FakeSlots: AE paints the item only, so yielding
            // to it leaves the count undrawn and the cell looks empty — draw it ourselves.
            if (info.slotClass().contains("FakeSlot")) {
                return false;
            }
            return !info.playerSide();
        }
        // 认不出槽位的格子（ExtendedAE 综合终端那类自绘网格）也一律不画。
        return true;
    }

    private static boolean ultrastacks$isCraftingMatrixSlot(AbstractContainerScreen<?> container, int itemX, int itemY) {
        return "appeng.menu.slot.CraftingMatrixSlot"
                .equals(SlotLookup.find(container.getMenu(), itemX, itemY).slotClass());
    }

    /**
     * 这一格是不是已经由我们的批次预览接管了。
     *
     * <p>先看渲染上下文给的槽位，拿不到就按坐标在菜单里找。
     */
    private static boolean ultrastacks$anvilResultSilent(int itemX, int itemY) {
        var minecraft = Minecraft.getInstance();
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
        // 铁砧产物格：批次只有 1 时这一格不该有任何数字（原版也不画 1）。
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
        return ResultPreview.previewTotal(container, slot) > 0L;
    }

    /** 是不是合成产物格（AE 的合成产物槽、原版/精妙的第 3 个容器槽），判定统一交给 ResultPreview。 */
    private static boolean ultrastacks$isResultSlot(Slot slot) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            return ResultPreview.isResultSlotFor(containerScreen, slot);
        }
        return false;
    }

    /**
     * 把原版那串原始数字换成缩写，再交给原版自己画。
     *
     * <p>自己调 {@code text} 在 26.1.2 的渲染状态里颜色/层序都可能不对（上一版就因为 ARGB 全透明而
     * 整片看不见）。这里只改内容、不改绘制者，坐标与颜色都由原版算，避免再踩同一类坑。
     *
     * <p>Only the string content is replaced; vanilla still does the drawing, so colour and ordering
     * stay exactly as vanilla computes them.
     */
    @Redirect(method = "itemCount", at = @At(
            value = "INVOKE",
            target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"
    ))
    private static String ultrastacks$abbreviateCount(int count) {
        if (count <= 0) {
            // AE 在按住 Shift 拖格子时会拿一个数量为 0 的“幽灵栈”去绘制，原版 itemCount 只要
            // 数量 != 1 就会把这个 0 画出来，于是鼠标上多出一个数字，松手才消失。空串即不画。
            // While Shift-dragging AE hands a zero-count ghost stack to the vanilla counter, which
            // paints that "0" next to the cursor until the button is released. An empty string draws nothing.
            return "";
        }
        if (!UltraStacksConfig.displayModule() || count <= 99) {
            return Integer.toString(count);
        }
        String shown = CountFormat.format(count);
        return shown.isEmpty() ? Integer.toString(count) : shown;
    }

    @Redirect(method = "itemCount", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"
    ))
    private void ultrastacks$compactCount(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color,
                                          boolean dropShadow,
                                          Font itemFont, ItemStack stack, int itemX, int itemY,
                                          @Nullable String countText) {
        // 产物格上我们要画「一批能做多少」，原版那串「一次的量」就别再画了。
        if (UltraStacksConfig.displayModule() && ultrastacks$anvilResultSilent(itemX, itemY)) {
            return;
        }
        if (UltraStacksConfig.displayModule() && ultrastacks$previewOwnsSlot(itemX, itemY)) {
            return;
        }
        if (net.minecraft.client.Minecraft.getInstance().screen
                instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) {
            String screenName = net.minecraft.client.Minecraft.getInstance().screen == null ? "null"
                    : net.minecraft.client.Minecraft.getInstance().screen.getClass().getSimpleName();
            com.zaixiayesheng.ultrastacks.DiagLog.lineThrottled(
                    "creative.count." + screenName + "." + stack.getCount() + "." + itemX + "." + itemY,
                    "[diag] creative count: stack=" + stack.getCount() + " text=" + text + " countText=" + countText
                            + " textAt=" + x + "," + y + " itemAt=" + itemX + "," + itemY
                            + " foreign=" + ultrastacks$isForeignGridSlot(stack, itemX, itemY)
                            + " skip=" + ultrastacks$skipForeignCount(stack, itemX, itemY) + " screen=" + screenName);
        }
        // 26.1.2 有些界面不给文本（传 null），原版照样画数字；这里补上，否则整片区域一个数都没有。
        // Some 26.1.2 screens pass null here even though vanilla still draws the count; fall back to the
        // raw number so those slots do not lose their label entirely.
        String base = text != null ? text : Integer.toString(stack.getCount());
        if (!UltraStacksConfig.displayModule()) {
            graphics.text(font, base, x, y, color, dropShadow);
            return;
        }
        if (stack.getCount() <= 1) {
            return;
        }
        if (stack.getCount() <= 99) {
            // 小数量照原样画，但坐标改用槽位坐标：创造栏与模组标签页给进来的 x,y 是滚动区坐标，
            // 照抄会画到看不见的地方，那一页就「一个数字都没有」。
            // Small counts stay verbatim, but anchored to the slot: creative/mod tabs hand us scroll
            // coordinates, and copying them paints the label off-screen so the page looks count-less.
            graphics.text(font, base, itemX + 17 - font.width(base), itemY + 9, color, dropShadow);
            return;
        }
        // 传了非空文本 = 调用方自己排好了字，照它的画；传空串是「这格我自己画」，一笔都不画。
        // 有的界面把「原始数字」当成自定义文本传进来（例如 AE 无线终端的副手格传 "65536"），
        // 那不是它排好的字，只是把数量塞给我们照抄，这种照样缩写。
        boolean rawCountText = countText != null && countText.equals(Integer.toString(stack.getCount()));
        if (countText != null && !countText.isEmpty() && !rawCountText) {
            graphics.text(font, base, x, y, color, dropShadow);
            return;
        }
        if (countText != null && countText.isEmpty()) {
            // 精妙与 RS 传空串表示「这格我自己画」，交给它们；AE 传空串只是「别画文本」，
            // 它自己也不画数量，所以 AE 的格子要我们自己补，否则一格看着是空的。
            // Sophisticated/RS pass "" to mean "I paint this cell"; AE passes "" to mean "no text at all",
            // and then paints nothing itself, so AE slots need our label or they look count-less.
            var current = net.minecraft.client.Minecraft.getInstance().screen;
            String screenName = current == null ? "" : current.getClass().getName();
            if (screenName.startsWith("net.p3pp3rf1y.") || screenName.startsWith("com.refinedmods.")) {
                return;
            }
            String own = stack.getCount() > 99 ? com.zaixiayesheng.ultrastacks.display.CountFormat.format(stack.getCount())
                    : Integer.toString(stack.getCount());
            if (own.isEmpty()) {
                own = Integer.toString(stack.getCount());
            }
            int ownWidth = font.width(own);
            if (ownWidth <= 16) {
                graphics.text(font, own, itemX + 17 - ownWidth, itemY + 9, 0xFFFFFFFF, true);
            } else {
                float ownScale = 16f / (float) ownWidth;
                graphics.pose().pushMatrix();
                graphics.pose().translate((float) (itemX + 17), (float) (itemY + 16));
                graphics.pose().scale(ownScale, ownScale);
                graphics.text(font, own, -ownWidth, -8, 0xFFFFFFFF, true);
                graphics.pose().popMatrix();
            }
            return;
        }
        if (stack.getCount() > 1) {
            var sc = Minecraft.getInstance().screen;
            String sn = sc == null ? "null" : sc.getClass().getName();
            if (sn.startsWith("appeng.") || sn.startsWith("de.mari_023.ae2wtlib.")
                    || sn.startsWith("com.glodblock.github.extendedae.")) {
                com.zaixiayesheng.ultrastacks.DiagLog.lineThrottled(
                        "ae.slot." + itemX + "." + itemY + "." + stack.getCount(),
                        "[diag] ae slot: screen=" + sn + " count=" + stack.getCount()
                                + " text=" + (countText == null ? "null" : "\"" + countText + "\"")
                                + " slot=" + SlotLookup.find(((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) sc).getMenu(), itemX, itemY)
                                + " at=" + itemX + "," + itemY);
            }
        }
        // AE 传 null 文本 = 它自己没排字，等着我们画（ME 接口那排配置格就是这种）；只有调用方
        // 明确给了文本时，才轮到「是不是它自己画的」这套让位判断，否则整排格子会一个数都没有。
        // AE passes null when it has not laid out any text and expects the count drawn for it (the ME
        // interface config row). Only when the caller supplied text does the "belongs to someone else"
        // check apply, otherwise whole rows lose their labels.
        if (countText == null && ultrastacks$aePaintedCell(itemX, itemY)) {
            return;
        }
        if (countText != null && ultrastacks$skipForeignCount(stack, itemX, itemY)) {
            return;
        }
        if (countText != null && ultrastacks$isForeignGridSlot(stack, itemX, itemY)) {
            // 让位给「外部网格」时也还是按槽位坐标画：原版给的 x,y 在这些自绘列表里是滚动区坐标，
            // 照抄会画到看不见的地方（创造栏那格就是这么消失的）。
            // Even when yielding to a foreign grid, draw at the slot coordinates: the x/y vanilla hands us
            // are scroll-list coordinates there, and copying them paints the label off-screen.
            return;
        }

        String shown = CountFormat.format(stack.getCount());
        com.zaixiayesheng.ultrastacks.DiagLog.lineThrottled("count.draw." + stack.getCount(),
                "[diag] count draw: stack=" + stack.getCount() + " shown=" + shown + " base=" + base
                        + " at=" + itemX + "," + itemY + " screen="
                        + (Minecraft.getInstance().screen == null ? "null"
                                : Minecraft.getInstance().screen.getClass().getSimpleName()));
        if (shown.isEmpty()) {
            graphics.text(font, base, x, y, color, dropShadow);
            return;
        }
        // 有缩放的那条路才塞得进格子：交给 CountRenderer 自己画（颜色已改成不透明白）。
        CountRenderer.drawForSlot(graphics, font, shown, itemX, itemY);
        return;
    }
    /**
     * AE 自己用坐标画出来的存储列表格（不是菜单里的真实槽位）——那些格子的数量由 AE 自己写，
     * 我们再补一层就会叠出两个数字。认得出来是真实槽位的才归我们。
     *
     * <p>AE paints its storage grid with free coordinates rather than menu slots; those cells already
     * carry AE's own count, so drawing another one on top would duplicate it. Only real slots are ours.
     */
    private static boolean ultrastacks$aePaintedCell(int itemX, int itemY) {
        var screen = Minecraft.getInstance().screen;
        if (!(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container)) {
            return false;
        }
        String name = screen.getClass().getName();
        boolean aeScreen = name.startsWith("appeng.") || name.startsWith("de.mari_023.ae2wtlib.")
                || name.startsWith("com.glodblock.github.extendedae.");
        return aeScreen && !SlotLookup.find(container.getMenu(), itemX, itemY).found();
    }

}
