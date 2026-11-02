package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.display.ResultPreview;
import com.zaixiayesheng.ultrastacks.display.SlotRenderContext;
import com.zaixiayesheng.ultrastacks.display.CountFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 接管物品栏与容器里那串计数文本的绘制。
 *
 * <p>计数不超过 99、显示模块关掉、调用方自己给了 {@code customText}，或者原版传进来的是 {@code null}，都照原样画。超过 99 时把数字换成 {@link CountFormat#format(long)} 的结果——默认 1000 以下仍是完整数字，之上缩写成 k / m / b，阈值由 {@code abbreviateAbove} 控制。
 *
 * <p>缩写后字宽变窄，所以按两边字宽之差把起点右移，保持原版的右对齐。
 *
 * <p>Takes over the drawing of the count text in inventories and containers.
 *
 * <p>A count of at most 99, a disabled display module, a caller-supplied {@code customText} or a
 * {@code null} text from vanilla all fall through to the original drawing. Above 99 the digits are
 * replaced by {@link CountFormat#format(long)}: full digits below 1000 by default and k / m / b
 * abbreviations above that, with the threshold controlled by {@code abbreviateAbove}.
 *
 * <p>An abbreviation is narrower than the text it replaces, so the starting point is shifted right
 * by the difference in string width to keep vanilla's right alignment.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    /** 槽位留给数量的横向像素。 / Horizontal pixels a slot leaves for the count. */
    private static final int SLOT_TEXT_WIDTH = 16;


    /**
     * 判断这一格的数量该不该由别人显示。
     *
     * <p>只有两类界面会抢数量显示：AE 的终端、精妙的背包 / 存储、精致存储的网格。它们在自己的网格上画
     * 缩写数量，我们再叠一层就重复了。**除此之外的界面一律由我们画**——别的模组的箱子、桶、机器都不提供
     * 数量显示，让位只会让大堆掉回原版的纯数字。
     *
     * <p>在这三类界面里也只让出网格那部分：玩家物品栏（容器是
     * {@code net.minecraft.world.entity.player.Inventory}）仍然归我们，不然开着终端看背包就没数量了。
     *
     * <p>槽位先按坐标认（{@code itemX/itemY} 就是槽位内相对坐标），对不上再按 {@code ItemStack} 引用认；
     * 两者都认不出（终端里临时拼出来的展示栈）就让位。
     *
     * <p>Only a few screens take over count rendering: AE terminals, Sophisticated backpacks/storage and
     * Refined Storage grids. Everywhere else — including other mods' chests and machines, which draw no counts
     * at all — the numbers stay ours. Inside those few screens only the grid yields; player-inventory slots
     * ({@code net.minecraft.world.entity.player.Inventory}) remain ours so an open terminal still shows counts
     * for your own inventory. Slots are matched by coordinates first and stack reference second; anything
     * unmatched is a temporary terminal stack and yields too.</p>
     */
    @SuppressWarnings("unused")
    private static boolean ultrastacks$isForeignGridSlot(ItemStack stack, int itemX, int itemY) {
        net.minecraft.client.gui.screens.Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (!(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container)) {
            return false;
        }
        String name = screen.getClass().getName();
        if (!com.zaixiayesheng.ultrastacks.display.DisplayOwnership.screenDrawsOwnCounts(name)) {
            return false;
        }
        // AE 的合成格自己画原始数字（不缩写），这一格交给我们，让 7281 变成 7.2k。
        if (ultrastacks$isCraftingMatrixSlot(container, itemX, itemY)) {
            return false;
        }
        boolean carried = container.getMenu().getCarried() == stack;
        // 坐标查询走缓存，避免每帧对每个格子重扫整张槽位表。
        // Cached coordinate lookup instead of rescanning every slot for every cell each frame.
        // 优先用「正在画的槽位」：原版逐格绘制时我们已经记下了，连同一次查表都不用。
        // Prefer the slot currently being drawn; vanilla's own loop already told us, so no lookup.
        net.minecraft.world.inventory.Slot drawing = com.zaixiayesheng.ultrastacks.display.SlotRenderContext.current;
        boolean found;
        boolean playerSideSlot;
        if (drawing != null) {
            found = true;
            String drawingClass = drawing.getClass().getName();
            playerSideSlot = drawing.container instanceof net.minecraft.world.entity.player.Inventory
                    || drawingClass.contains("ArmorSlot")
                    || drawingClass.contains("OffhandSlot");
        } else {
            com.zaixiayesheng.ultrastacks.display.SlotLookup.Info info =
                    com.zaixiayesheng.ultrastacks.display.SlotLookup.find(container.getMenu(), itemX, itemY);
            found = info.found();
            playerSideSlot = info.playerSide();
        }
        boolean aeScreen = name.startsWith("appeng.") || name.startsWith("de.mari_023.ae2wtlib.")
                || name.startsWith("com.glodblock.github.extendedae.");
        if (aeScreen) {
            // AE 系界面里只画「玩家自己身上」的格子：物品栏、副手/护甲、手上拿着的那堆。
            // 其余（ME 列表、ExtendedAE 综合终端里那些认不出槽位的格子）一律不画，
            // 否则会叠在 AE 自己那层数字上，出现两位数字压在一起的乱码。
            // Inside AE screens only player-side cells are ours; anything else stays untouched, otherwise
            // our text lands on top of AE's own and the digits overlap.
            return !(playerSideSlot || carried);
        }
        if (playerSideSlot) {
            return false;
        }
        // 坐标对不上任何槽位的，是「界面开着的时候画面底部那排 HUD 快捷栏」或其它界面外绘制，
        // 它们不属于任何槽位，也不该被让位，否则会原样画出未缩放的数字。
        // Anything that matches no slot by coordinate is the HUD hotbar (or another out-of-menu draw);
        // it belongs to no slot and must not be yielded, or it prints an unscaled raw count.
        return found && !carried && !playerSideSlot;
    }


    /**
     * AE 的存储列表不是真槽位，数字由它自己那层画；我们在同一坐标再画一次就会叠成两层。
     * 认不出槽位的 AE 格子交回给它，只有真槽（接口配置格等）才由我们补数字。
     *
     * <p>AE's storage list is not made of real slots and draws its own numbers; drawing again on the
     * same cell doubles them. Cells that match no slot go back to AE, only real slots are ours.
     */
    private static boolean ultrastacks$aeSelfDrawnCell(int itemX, int itemY) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.screen == null || minecraft.player == null) {
            return false;
        }
        String screenName = minecraft.screen.getClass().getName();
        boolean aeScreen = screenName.startsWith("appeng.") || screenName.startsWith("de.mari_023.ae2wtlib.")
                || screenName.startsWith("com.glodblock.github.extendedae.");
        if (!aeScreen) {
            return false;
        }
        return !com.zaixiayesheng.ultrastacks.display.SlotLookup
                .find(minecraft.player.containerMenu, itemX, itemY).found();
    }


    /**
     * 这一格是不是 AE 的合成格。
     *
     * <p>AE 合成格的数量由 AE 自己画成原始数字，7271 就写 7271；用户要的是和物品栏一致的缩写。
     * 只按坐标认格子，因为 AE 的槽位每次返回的栈对象可能是副本，按引用对不上。
     *
     * <p>Whether this grid position is one of AE's crafting matrix slots. AE prints their counts raw, so
     * those cells are handed back to us to abbreviate. Matched by position, since AE may hand out copies
     * of the stack instead of the same instance.</p>
     */
    /** 当前界面是不是精妙自己的界面（它自己画格子数字）。 / Is this one of Sophisticated's own screens? */
    private static boolean ultrastacks$isSophisticatedScreen() {
        net.minecraft.client.gui.screens.Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        return screen != null && screen.getClass().getName().startsWith("net.p3pp3rf1y.");
    }


    /**
     * 这一格的数量是不是干脆不该由任何人画（交给界面自己）。
     *
     * <p>只有 AE 系界面会这样：它的 ME 列表、合成矩阵自己画数字，我们和原版都不该再叠一层。
     * 玩家侧的格子（物品栏、护甲/副手、手里拿着的）除外，那些仍归我们。
     *
     * <p>Only AE screens skip entirely: their ME listing and crafting grid paint their own counts, so neither
     * vanilla nor we should add a second layer. Player-side cells stay ours.</p>
     */
    private static boolean ultrastacks$skipForeignCount(ItemStack stack, int itemX, int itemY) {
        net.minecraft.client.gui.screens.Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (!(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container)) {
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
        if ("appeng.menu.slot.CraftingMatrixSlot".equals(
                com.zaixiayesheng.ultrastacks.display.SlotLookup.find(container.getMenu(), itemX, itemY).slotClass())) {
            return false;
        }
        com.zaixiayesheng.ultrastacks.display.SlotLookup.Info info =
                com.zaixiayesheng.ultrastacks.display.SlotLookup.find(container.getMenu(), itemX, itemY);
        if (info.found()) {
            return !info.playerSide();
        }
        // 认不出槽位的格子（ExtendedAE 综合终端那类自绘网格）也一律不画：
        // 界面自己会画，我们插一手只会叠字。
        // Cells that cannot be matched to a menu slot (ExtendedAE's self-drawn grids) also draw nothing —
        // the screen paints those counts itself and a second layer only overlaps.
        return true;
    }

    private static boolean ultrastacks$isCraftingMatrixSlot(
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container, int itemX, int itemY) {
        return "appeng.menu.slot.CraftingMatrixSlot".equals(
                com.zaixiayesheng.ultrastacks.display.SlotLookup.find(container.getMenu(), itemX, itemY).slotClass());
    }

    private static int shownWidth(String text, Font font) {
        return font.width(text);
    }

    /**
     * 这一格是不是已经由我们的批次预览接管了。
     *
     * <p>先看渲染上下文给的槽位，拿不到（精妙自己的 renderSlot 不给上下文）就按坐标在菜单里找。
     * Whether the batch preview already owns this cell: prefer the slot handed over by renderSlot and fall
     * back to matching by position for screens that draw their own slots.
     */
    private static boolean ultrastacks$anvilResultSilent(int itemX, int itemY) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (!(minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) {
            return false;
        }
        if (!(screen.getMenu() instanceof net.minecraft.world.inventory.AnvilMenu)) {
            return false;
        }
        net.minecraft.world.inventory.Slot slot = SlotRenderContext.current;
        if (slot == null || slot.index != 2) {
            slot = null;
            for (net.minecraft.world.inventory.Slot candidate : screen.getMenu().slots) {
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
        net.minecraft.client.gui.screens.Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (!(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container)) {
            return false;
        }
        net.minecraft.world.inventory.Slot slot = com.zaixiayesheng.ultrastacks.display.SlotRenderContext.current;
        if (slot == null) {
            for (net.minecraft.world.inventory.Slot candidate : container.getMenu().slots) {
                if (candidate.x == itemX && candidate.y == itemY) {
                    slot = candidate;
                    break;
                }
            }
        }
        if (slot == null || slot.getItem().isEmpty()) {
            // 精妙自己的槽位不在菜单里，退回到「预览这一帧画过这格」的记录。
            // Sophisticated keeps its slots outside the menu, so fall back to the cells the preview drew.
            return com.zaixiayesheng.ultrastacks.display.ResultPreview.ownsCell(itemX, itemY);
        }
        // 只有合成台 / 背包 2x2 / 铁砧这几类菜单才有「产物预览」；创造模式物品栏第一格槽号也是 0，
        // 不区分会把整页创造物品的数量全压掉。
        // Only crafting-table / 2x2 / anvil menus carry a result preview; the creative inventory also
        // numbers its first slot 0, and matching on that index alone blanked every creative item.
        net.minecraft.world.inventory.AbstractContainerMenu menu = container.getMenu();
        boolean craftLike = menu instanceof net.minecraft.world.inventory.AnvilMenu
                || menu instanceof net.minecraft.world.inventory.CraftingMenu
                || menu instanceof net.minecraft.world.inventory.InventoryMenu;
        // 产物格的认定交给 ResultPreview：精妙背包的存储格槽号同样很小，只按槽号认会把材料当产物格画数字。
        // Result-slot identity comes from ResultPreview: backpack storage cells also use small container
        // indices, and matching on the index alone painted batch numbers onto raw materials.
        if (!ultrastacks$isResultSlot(slot) && !craftLike) {
            return com.zaixiayesheng.ultrastacks.display.ResultPreview.ownsCell(itemX, itemY);
        }
        if (com.zaixiayesheng.ultrastacks.display.ResultPreview.previewTotal(container, slot) > 0L) {
            return true;
        }
        return com.zaixiayesheng.ultrastacks.display.ResultPreview.ownsCell(itemX, itemY);
    }

    /** 是不是真正的合成产物格（AE 的合成产物槽、原版/精妙的第 3 个容器槽）。 */
    private static boolean ultrastacks$isResultSlot(net.minecraft.world.inventory.Slot slot) {
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
        // 产物格上我们要画「一批能做多少」，原版那串「一次的量」就别再画了，否则两个数字叠在一起。
        // 精妙自己画界面时不经过原版 renderSlot，拿不到 SlotRenderContext，所以再按坐标兜底认一次格。
        // The result slot gets our batch number, so the vanilla one-craft count is suppressed here; the
        // Sophisticated screens never set SlotRenderContext, so fall back to matching the slot by position.
        // 铁砧产物格：批次只有 1 时这一格不该有任何数字（原版也不画 1），所以连原版那串一起压掉。
        // The anvil result slot gets no number at all when the batch is one — vanilla does not draw a
        // single-item count either, so anything that would still print one is suppressed here.
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
            return graphics.drawString(font, text, x, y - 1, color, dropShadow);
        }
        // 传了非空文本 = 调用方自己排好了字，照它的画。
        // 传空串是两种意思：精妙表示"我自己画这格"，AE 表示"我不想画"——所以按界面区分，
        // 把 AE 的空串接过来画缩写，精妙的空串继续让开。
        // A non-empty text means the caller laid out its own string. An empty string means two
        // different things: Sophisticated draws the cell itself, AE simply opts out — so the empty
        // string is only yielded back to Sophisticated.
        // 有的界面把「原始数字」当成自定义文本传进来（例如 AE 无线终端的副手格传 "65536"），
        // 那不是它排好的字，只是把数量塞给我们照抄。这种照样缩写；真正的自定义文本才放行。
        // Some screens pass the raw count as custom text (AE's wireless terminal does this for the
        // offhand slot, literally "65536"). That is not a laid-out label — abbreviate it too.
        boolean rawCountText = customText != null && customText.equals(Integer.toString(stack.getCount()));
        if (customText != null && !customText.isEmpty() && !rawCountText) {
            return graphics.drawString(font, text, x, y - 1, color, dropShadow);
        }
        // 空串是"这格我自己画"（精妙自己会补数字），这里一笔都不画，免得和它叠成两层。
        // An empty string means "I draw this cell"; draw nothing so it never doubles with our text.
        if (customText != null && customText.isEmpty()) {
            return 0;
        }
        if (customText != null && ultrastacks$skipForeignCount(stack, itemX, itemY)) {
            return 0;
        }
        if (customText != null && ultrastacks$isForeignGridSlot(stack, itemX, itemY)) {
            // 认不出槽位的 AE 格子是它自己那层在画数字，我们别再叠一层。
            // Cells that match no slot belong to AE's own rendering; do not stack a second number on them.
            if (ultrastacks$aeSelfDrawnCell(itemX, itemY)) {
                return 0;
            }
            return graphics.drawString(font, text, x, y - 1, color, dropShadow);
        }

        String shown = CountFormat.format(stack.getCount());
        if (shown.isEmpty()) {
            return graphics.drawString(font, text, x, y - 1, color, dropShadow);
        }
        // 坐标、缩放、颜色全部照抄精妙的 renderStackCount，保证同一排里两边字号一致。
        // Position, scale and colour copied from Sophisticated's renderStackCount so both mods'
        // numbers are the same size on one screen.
        com.zaixiayesheng.ultrastacks.display.CountRenderer.drawForSlot(graphics, font, shown, itemX, itemY);
        return shownWidth(text, font);
    }
}
