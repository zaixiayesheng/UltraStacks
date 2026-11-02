package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.DiagLog;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.client.UnlockKey;
/**
 * 在产物格上画「这一批能做多少个」。
 *
 * <p>产物格本身永远只放一次的量（原版语义，取一次扣一次材料），所以这里只画预览数字：不按 Shift 显示
 * 一次的量，按 Shift 显示一批（默认 64），再按住解锁键显示材料允许的上限。铁砧按左槽剩料算，合成按
 * 配方的材料算（比如铁块产 9 锭，一批就是 9×64）。
 *
 * <p>Draws a batch preview on the result slot. The slot itself still holds a single craft's output, so the
 * number is purely visual: one craft normally, a batch (64 by default) while Shift is held, and as much
 * as the materials allow while the unlock key is held.
 */
public final class ResultPreview {
    private ResultPreview() {
    }
    /**
     * 找出当前 {@link GuiGraphicsExtractor} 是否已经平移到了界面原点。
     *
     * <p>在原版 {@code renderSlot} 里，绘制时姿态已经平移过 {@code guiLeft/guiTop}，此时槽位坐标
     * 直接可用；AE 与精妙这些自己画界面的地方是在未平移的姿态里调用，需要自己补上界面原点。
     * Returns the screen origin to add to slot coordinates: zero when the pose is already translated.
     */
    /** 预览画过的格子（屏幕坐标），用来压掉同一格上的原版数字。 */
    private static final java.util.Map<Long, Long> DRAWN_CELLS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Long, net.minecraft.world.item.ItemStack> DRAWN_STACKS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static long cellKey(int x, int y) {
        return ((long) x << 32) ^ (y & 0xFFFFFFFFL);
    }

    private static net.minecraft.world.item.ItemStack lastDrawnStack = net.minecraft.world.item.ItemStack.EMPTY;

    public static void rememberCell(int x, int y, long total) {
        if (DRAWN_CELLS.size() > 256) {
            DRAWN_CELLS.clear();
        }
        DRAWN_CELLS.put(cellKey(x, y), total);
        DRAWN_STACKS.put(cellKey(x, y), lastDrawnStack);
    }

    /**
     * 这一格上已经画着我们的批次数字了吗（供上层压掉原版那串「一次的量」）。
     *
     * <p>Whether our batch number was drawn at this screen position last frame, so the vanilla
     * one-craft count can be suppressed there.
     */
    public static boolean ownsCell(int x, int y) {
        // 允许 1 像素误差：精妙画背景框时会往左上各偏一格像素，调用方给的图标坐标有时是那圈框。
        // Allow a one-pixel slack: Sophisticated offsets its slot frame by one pixel in each direction.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (DRAWN_CELLS.containsKey(cellKey(x + dx, y + dy))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 这一格上我们画过的合计量，没有则 -1。 */
    public static long drawnTotalAt(int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Long v = DRAWN_CELLS.get(cellKey(x + dx, y + dy));
                if (v != null) {
                    return v;
                }
            }
        }
        return -1L;
    }

    /**
     * 鼠标底下那一格我们画了多少（精妙的槽位不在菜单里，只能按屏幕坐标反查）。
     *
     * <p>How much our preview drew under the cursor: the upgrade tabs keep their slots outside the
     * menu, so the cell is looked up by screen position instead.
     */
    /** 鼠标下那一格我们画的物品（没有则空栈）。 */
    public static net.minecraft.world.item.ItemStack stackUnderMouse() {
        var mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        double guiW = mc.getWindow().getGuiScaledWidth();
        double guiH = mc.getWindow().getGuiScaledHeight();
        double winW = Math.max(1, mc.getWindow().getScreenWidth());
        double winH = Math.max(1, mc.getWindow().getScreenHeight());
        int gx = (int) Math.round(mc.mouseHandler.getScaledXPos(mc.getWindow()));
        int gy = (int) Math.round(mc.mouseHandler.getScaledYPos(mc.getWindow()));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                net.minecraft.world.item.ItemStack v = DRAWN_STACKS.get(cellKey(gx + dx, gy + dy));
                if (v != null) {
                    return v;
                }
            }
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    public static long totalUnderMouse(AbstractContainerScreen<?> screen) {
        var mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return -1L;
        }
        double guiW = mc.getWindow().getGuiScaledWidth();
        double guiH = mc.getWindow().getGuiScaledHeight();
        double winW = Math.max(1, mc.getWindow().getScreenWidth());
        double winH = Math.max(1, mc.getWindow().getScreenHeight());
        int gx = (int) Math.round(mc.mouseHandler.getScaledXPos(mc.getWindow()));
        int gy = (int) Math.round(mc.mouseHandler.getScaledYPos(mc.getWindow()));
        return drawnTotalAt(gx, gy);
    }

    /**
     * 是否按住 Shift。26.1.2 删了 {@code Screen#hasShiftDown}，改为直接问 GLFW。
     *
     * <p>Whether Shift is held. 26.1.2 removed {@code Screen#hasShiftDown}, so GLFW is asked directly.
     */
    /**
     * 26.1.2 的客户端不再持有配方表：{@code Level#getRecipeManager} 已删除，客户端只剩
     * {@code RecipeAccess}（属性集 + 切石机配方），带材料表的 {@code RecipeManager} 留在服务端。
     * 单人生存里集成服务端还在，可以照旧查配方；纯客户端（多人服）拿不到，相关预览自动关闭。
     *
     * <p>26.1.2 removed {@code Level#getRecipeManager}; the client keeps only a {@code RecipeAccess}
     * (property sets and stonecutter recipes) and the ingredient tables live with the server's
     * {@code RecipeManager}. Single player can still reach it through the integrated server; on a
     * multiplayer client the previews that need a recipe stay off.
     */
    static net.minecraft.world.item.crafting.RecipeManager recipeManager() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        return server == null ? null : server.getRecipeManager();
    }

    private static boolean shiftHeld() {
        var window = Minecraft.getInstance().getWindow();
        if (window == null) {
            return false;
        }
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
            || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    static int[] origin(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen) {
        org.joml.Matrix3x2f matrix = graphics.pose();
        int tx = Math.round(matrix.m20());
        int ty = Math.round(matrix.m21());
        if (tx == 0 && ty == 0) {
            return new int[] {screen.getGuiLeft(), screen.getGuiTop()};
        }
        return new int[] {0, 0};
    }
    public static void draw(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, Slot slot) {
        long total = previewTotal(screen, slot);
        if (total <= 0L) {
            return;
        }
        ItemStack out = slot.getItem();
        // 产物格一直接管：哪怕算出来的数和槽内临时数量相同，也由我们画，避免精妙/原版在两种数字之间来回跳。
        // Always own the result slot: even when the computed amount equals the temporary slot count we draw it,
        // so the number does not flip between ours and the mod's own renderer.
        int[] origin = origin(graphics, screen);
        drawTotal(graphics, slot, out, total / Math.max(1, out.getCount()), origin[0], origin[1]);
    }

    /**
     * 算这一格产物「一批能做多少个」，只算不画，提示框也复用它。
     *
     * <p>Works out how many one batch would produce on this result slot, without drawing anything, so the
     * tooltip can show the same number.
     */
    public static long previewTotal(AbstractContainerScreen<?> screen, Slot slot) {
        // 精妙铁砧的产物槽不一定是原版 ResultSlot（它自己的容器里换过实现），所以别用类型卡，
        // 按「精妙界面 + 可持久化铁砧菜单的第 2 号槽」认定；类型卡死会让提示框永远拿不到合计。
        // The anvil result slot inside Sophisticated is not always a vanilla ResultSlot, so match on
        // "Sophisticated screen + slot 2 of the persistable anvil menu" instead of the class.
        if (screen.getClass().getName().startsWith("net.p3pp3rf1y.") && isSophAnvilResult(slot)) {
            long total = sophAnvilTotal(screen);
            if (total > 0L) {
                return total;
            }
        }
        if (AeCraftPreview.isAeMenu(screen.getMenu())) {
            // AE 只认产物格那一格，别把合计行带到别的物品上。
            // AE only reports the crafting result, so the batch line never leaks onto other stacks.
            return AeCraftPreview.previewTotal(slot);
        }
        if (!UltraStacksConfig.displayModule() || screen == null || slot == null) {
            return -1L;
        }
        ItemStack out = slot.getItem();
        if (out.isEmpty()) {
            return -1L;
        }
        AbstractContainerMenu menu = screen.getMenu();
        if (menu == null) {
            return -1L;
        }
        boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld();
        boolean shift = shiftHeld();
        long limit = Math.max(1, UltraStacksConfig.bulkCraftLimit());
        if (slot == sophCraftingResultSlot(screen)) {
            return sophCraftingTotal(screen);
        }
        if (menu instanceof AnvilMenu anvil && slot.index == 2) {
            ItemStack left = anvil.getSlot(0).getItem();
            if (left.isEmpty() || left.getCount() <= 1) {
                return -1L;
            }
            long available = left.getCount();
            ItemStack additive = anvil.getSlot(1).getItem();
            if (!additive.isEmpty()) {
                available = Math.min(available, additive.getCount());
            }
            long batch = unlocked ? available : (shift ? Math.min(limit, available) : 1L);
            // 一次就是一次的时候不画：铁砧不按 Shift 只修一件，格子里本来就有那个 1。
            // With no batch requested the anvil repairs exactly one item, so leave the slot alone.
            if (batch <= 1L) {
                return -1L;
            }
            return out.getCount() * batch;
        }
        if (slot.index != 0) {
            return -1L;
        }
        CraftingInput input = craftingInput(menu);
        var level = Minecraft.getInstance().level;
        if (input == null || level == null) {
            return -1L;
        }
        var manager = recipeManager();
        if (manager == null) {
            return -1L;
        }
        var holder = manager.getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        if (holder == null) {
            return -1L;
        }
        long crafts = maxCrafts(holder.value().placementInfo().ingredients(), gridStacks(menu));
        if (crafts <= 1L) {
            return -1L;
        }
        long batch = unlocked ? crafts : (shift ? Math.min(limit, crafts) : 1L);
        return out.getCount() * batch;
    }

    /**
     * 这一格是不是「真正的产物槽」——精妙背包的存储格也会出现第 3 个容器槽号，只按槽号认会把材料当产物。
     *
     * <p>26.1.2 不带 AE2 的编译期依赖，{@code appeng.menu.slot.CraftingTermSlot} 只能按类名认（含子类）。
     *
     * <p>Whether this slot really is a result slot: backpack storage slots also use small container
     * indices, so index alone would paint numbers onto raw materials. AE2 is not a compile-time
     * dependency in 26.1.2, so its slot is matched by class name.
     */
    public static boolean isResultSlotFor(AbstractContainerScreen<?> screen, Slot slot) {
        if (slot == null) {
            return false;
        }
        if (classNamed(slot.getClass(), "appeng.menu.slot.CraftingTermSlot")) {
            return true;
        }
        if (screen != null && screen.getClass().getName().startsWith("net.p3pp3rf1y.")) {
            return slot == sophCraftingResultSlot(screen) || isSophAnvilResult(slot);
        }
        return slot.getContainerSlot() == 2
                && !(slot.container instanceof net.minecraft.world.entity.player.Inventory);
    }

    /** 精妙铁砧的产物槽：容器是不是那个可持久化铁砧菜单，槽号 2。 */
    private static boolean isSophAnvilResult(Slot slot) {
        // 精妙铁砧的产物槽容器就是原版的 ResultContainer（PersistableAnvilMenu 名字只出现在菜单上，
        // 不在容器上），所以按容器类型认，别按类名认。
        // The anvil result slot's container is a plain ResultContainer; the "PersistableAnvilMenu"
        // name lives on the menu, never on the container.
        return slot != null && slot.getContainerSlot() == 2
                && slot.container instanceof net.minecraft.world.inventory.ResultContainer;
    }

    /** 类名认型（含父类），用来在没有编译期依赖时近似 {@code instanceof}。 */
    private static boolean classNamed(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (name.equals(c.getName())) {
                return true;
            }
        }
        return false;
    }

    /** 精妙界面里按鼠标坐标反查这一格，命中就返回我们画过的合计。 */
    public static long previewTotalUnderMouse(AbstractContainerScreen<?> screen) {
        return UltraStacksConfig.displayModule() ? totalUnderMouse(screen) : -1L;
    }

    private static CraftingInput craftingInput(AbstractContainerMenu menu) {
        List<ItemStack> items = new ArrayList<>(9);
        for (int i = 0; i < menu.slots.size() && items.size() < 9; i++) {
            Slot slot = menu.getSlot(i);
            if (slot.container instanceof CraftingContainer && slot.getContainerSlot() < 9) {
                items.add(slot.getItem().copy());
            }
        }
        if (items.size() < 9) {
            return null;
        }
        return CraftingInput.of(3, 3, items);
    }
    /**
     * 合成格里的材料：原版合成只消耗格子里那点东西，背包里的同类物品不算数。
     *
     * <p>The stacks currently sitting in the crafting grid: vanilla crafting only consumes these, so the
     * player's inventory must not inflate the estimate.
     */
    private static List<ItemStack> gridStacks(AbstractContainerMenu menu) {
        List<ItemStack> stacks = new ArrayList<>(9);
        for (Slot slot : menu.slots) {
            if (slot.container instanceof CraftingContainer && slot.getContainerSlot() < 9) {
                stacks.add(slot.getItem());
            }
        }
        return stacks;
    }

    private static long maxCrafts(List<Ingredient> ingredients, List<ItemStack> grid) {
        return groupedCrafts(ingredients, ingredient -> {
            long have = 0L;
            for (ItemStack stack : grid) {
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    have += stack.getCount();
                }
            }
            return have;
        });
    }
    /**
     * 按材料「种类」分组算能做几次：同一个配方里出现多份同种材料时（比如四格木板），必须把份数当除数，
     * 否则会把库存重复计入，算出「128 个材料能做 130k 次」这种荒唐数。
     *
     * <p>Groups ingredients by kind and divides by how many of that kind one craft needs, so repeated
     * ingredients are not counted once per entry.
     */
    static long groupedCrafts(List<Ingredient> ingredients,
            java.util.function.ToLongFunction<Ingredient> available) {
        java.util.Map<String, long[]> groups = new java.util.LinkedHashMap<>();
        java.util.Map<String, Ingredient> representatives = new java.util.LinkedHashMap<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            String key = keyOf(ingredient);
            groups.computeIfAbsent(key, k -> new long[] {0L, 0L})[0]++;
            representatives.putIfAbsent(key, ingredient);
        }
        for (var entry : representatives.entrySet()) {
            groups.get(entry.getKey())[1] = available.applyAsLong(entry.getValue());
        }
        long crafts = Long.MAX_VALUE;
        for (long[] group : groups.values()) {
            long perCraft = group[0];
            long have = group[1];
            if (perCraft <= 0L) {
                continue;
            }
            crafts = Math.min(crafts, have / perCraft);
        }
        return crafts == Long.MAX_VALUE ? 0L : crafts;
    }
    private static String keyOf(Ingredient ingredient) {
        StringBuilder builder = new StringBuilder();
        ingredient.items().forEach(item -> builder.append(item.value().toString()).append(','));
        return builder.toString();
    }
    /**
     * 精妙背包/存储的合成升级页：结果槽和九格配方格都在内层容器里，按配方与库存算一批能做多少。
     *
     * <p>The crafting upgrade tab keeps its result slot and 3×3 grid inside the upgrade container, so the
     * batch size is derived from the recipe plus whatever the menu exposes as ingredients.
     */
    public static void drawSophisticatedCrafting(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen) {
        if (!UltraStacksConfig.displayModule() || screen.getMenu() == null) {
            return;
        }
        try {
            Object container = findUpgradeContainer(screen.getMenu(), "upgrades.crafting.CraftingUpgradeContainer");
            if (container == null) {
                return;
            }
            java.lang.reflect.Field resultField = null;
            for (Class<?> c = container.getClass(); c != null && resultField == null; c = c.getSuperclass()) {
                try {
                    resultField = c.getDeclaredField("craftingResultSlot");
                } catch (NoSuchFieldException ignored) {
                    // 继续往上找 / keep walking up
                }
            }
            if (resultField == null) {
                return;
            }
            resultField.setAccessible(true);
            if (!(resultField.get(container) instanceof Slot result)) {
                return;
            }
            ItemStack out = result.getItem();
            if (out.isEmpty()) {
                return;
            }
            java.lang.reflect.Method slotsMethod = container.getClass().getMethod("getRecipeSlots");
            Object raw = slotsMethod.invoke(container);
            if (!(raw instanceof java.util.List<?> slots)) {
                return;
            }
            List<ItemStack> grid = new ArrayList<>(9);
            for (Object element : slots) {
                if (element instanceof Slot slot) {
                    grid.add(slot.getItem().copy());
                }
            }
            if (grid.size() != 9) {
                return;
            }
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            var manager = recipeManager();
            if (manager == null) {
                return;
            }
            var holder = manager
                .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), level)
                .orElse(null);
            if (holder == null) {
                return;
            }
            long crafts = maxCrafts(holder.value().placementInfo().ingredients(), grid);
            if (crafts <= 1) {
                return;
            }
            // 精妙产物槽里常常只摆一个，真正的“一次给多少”要看配方本身。
            // The upgrade tab shows a single sample, so one craft's output comes from the recipe.
            int perCraft = Math.max(1, holder.value().assemble(CraftingInput.of(3, 3, grid)).getCount());
            SOPH_PER_CRAFT.put(screen, perCraft);
            boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld();
            long limit = Math.max(1, UltraStacksConfig.bulkCraftLimit());
            long batch = unlocked ? crafts
                : (shiftHeld() ? Math.min(limit, crafts) : 1);
            com.zaixiayesheng.ultrastacks.DiagLog.lineThrottled("soph.craft." + batch,
                    "[diag] soph craft: crafts=" + crafts + " perCraft=" + perCraft + " batch=" + batch
                            + " slotCount=" + out.getCount() + " total=" + (perCraft * batch)
                            + " slot=" + result.x + "," + result.y + " gui=" + screen.getGuiLeft() + "," + screen.getGuiTop());
            drawTotal(graphics, result, out, batch, screen.getGuiLeft(), screen.getGuiTop(), perCraft);
        } catch (Throwable ignored) {
            // 反射失败就当拿不到 / reflection failure means no preview
        }
    }

    /**
     * 精妙合成升级页这一批能做多少（含配方产出数量）；拿不到返回 -1。
     *
     * <p>How much one batch makes on the Sophisticated crafting tab, recipe output included; -1 when
     * the tab cannot be resolved.
     */
    /** 记忆每种精妙合成页配方一次的产出数：合成完那一帧配方/材料可能暂时读不到，仍要显示「这个配方一次出几个」。
     *  Remembers the per-craft output of each Sophisticated crafting screen: right after a craft the recipe or the
     *  grid can be momentarily unreadable, yet the recipe's own output count must still be shown. */
    private static final java.util.Map<AbstractContainerScreen<?>, Integer> SOPH_PER_CRAFT = new java.util.WeakHashMap<>();

    public static long sophCraftingTotal(AbstractContainerScreen<?> screen) {
        String why = "ok";
        try {
            Object container = findUpgradeContainer(screen.getMenu(), "upgrades.crafting.CraftingUpgradeContainer");
            if (container == null) {
                DiagLog.lineThrottled("soph.total.nocontainer", "[diag] soph total: no CraftingUpgradeContainer, menu=" + screen.getMenu().getClass().getName());
            }
            if (container == null) {
                return -1L;
            }
            java.lang.reflect.Field resultField = null;
            for (Class<?> c = container.getClass(); c != null && resultField == null; c = c.getSuperclass()) {
                try {
                    resultField = c.getDeclaredField("craftingResultSlot");
                } catch (NoSuchFieldException ignored) {
                    // 继续往上找 / keep walking up
                }
            }
            if (resultField == null) {
                return -1L;
            }
            resultField.setAccessible(true);
            if (!(resultField.get(container) instanceof Slot result) || result.getItem().isEmpty()) {
                Integer cached = SOPH_PER_CRAFT.get(screen);
                return cached == null ? -1L : cached.longValue() * ultrastacks$batch(1);
            }
            Object raw = container.getClass().getMethod("getRecipeSlots").invoke(container);
            if (!(raw instanceof java.util.List<?> slots)) {
                return -1L;
            }
            List<ItemStack> grid = new ArrayList<>(9);
            for (Object element : slots) {
                if (element instanceof Slot slot) {
                    grid.add(slot.getItem().copy());
                }
            }
            if (grid.size() != 9) {
                return -1L;
            }
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return -1L;
            }
            var manager = recipeManager();
            if (manager == null) {
                return -1L;
            }
            var holder = manager
                .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), level).orElse(null);
            if (holder == null) {
                return -1L;
            }
            long crafts = maxCrafts(holder.value().placementInfo().ingredients(), grid);
            if (crafts <= 0) {
                Integer cached = SOPH_PER_CRAFT.get(screen);
                return cached == null ? -1L : cached.longValue() * ultrastacks$batch(1);
            }
            int perCraft = Math.max(1, holder.value().assemble(CraftingInput.of(3, 3, grid)).getCount());
            // 开了「自动补货」时，格子里的材料会被背包里的同类物品续上，预览要按续得上多少算，
            // 否则永远只显示一次的量。上限是显示分母，不夹这里。
            // With auto-refill on, the grid keeps being topped up from the backpack, so the preview has
            // to count what can actually be refilled; the stack limit is the on-screen denominator, not
            // a bound on how much can be crafted.
            crafts = crafts + refillCrafts(container, screen.getMenu(),
                    holder.value().placementInfo().ingredients(), grid);
            boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld();
            long limit = Math.max(1, UltraStacksConfig.bulkCraftLimit());
            long batch = unlocked ? crafts
                : (shiftHeld() ? Math.min(limit, crafts) : 1);
            DiagLog.lineThrottled("soph.total." + perCraft + "." + batch,
                    "[diag] soph total: crafts=" + crafts + " perCraft=" + perCraft + " batch=" + batch
                            + " total=" + (perCraft * batch) + " screen=" + screen.getClass().getSimpleName());
            return (long) perCraft * batch;
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    /**
     * 「自动补货」能补出多少次：按每个材料在背包里还有多少除以每轮用量算，取最紧的那个。
     *
     * <p>26.1.2 的精妙库存走 NeoForge 传输 API（{@code ItemStacksResourceHandler}）：槽位数是
     * {@code size()}（1.20.1 里叫 {@code getSlots()}），取料仍是 {@code getStackInSlot(int)}。
     *
     * <p>How many extra crafts auto-refill can top up: each ingredient's supply in the backpack divided
     * by how many of it one craft eats, the tightest one wins. Returns 0 when refill is off. In 26.1.2
     * the sophisticated inventory is a NeoForge transfer handler, so the slot count is {@code size()}
     * (was {@code getSlots()} in 1.20.1).
     */
    private static long refillCrafts(Object container, AbstractContainerMenu menu, List<Ingredient> ingredients,
            List<ItemStack> grid) {
        try {
            java.lang.reflect.Method refill = null;
            for (Class<?> c = container.getClass(); c != null && refill == null; c = c.getSuperclass()) {
                try {
                    refill = c.getDeclaredMethod("shouldRefillCraftingGrid");
                } catch (NoSuchMethodException ignored) {
                    // 继续往上找 / keep walking up
                }
            }
            if (refill == null) {
                return 0L;
            }
            refill.setAccessible(true);
            if (!Boolean.TRUE.equals(refill.invoke(container))) {
                return 0L;
            }
            // 背包库存挂在菜单上：menu.getStorageWrapper().getInventoryHandler()。
            // The backpack inventory lives on the menu: menu.getStorageWrapper().getInventoryHandler().
            Object wrapper;
            try {
                wrapper = menu.getClass().getMethod("getStorageWrapper").invoke(menu);
            } catch (NoSuchMethodException e) {
                return 0L;
            }
            if (wrapper == null) {
                return 0L;
            }
            Object storage;
            try {
                storage = wrapper.getClass().getMethod("getInventoryHandler").invoke(wrapper);
            } catch (NoSuchMethodException e) {
                return 0L;
            }
            if (storage == null) {
                return 0L;
            }
            // 26.1.2 的传输型 handler 只有 size()；getSlots() 留着当旧版退路。
            // The 26.1.2 transfer handler exposes size(); getSlots() stays as the older fallback.
            java.lang.reflect.Method getSlots = null;
            for (String candidate : new String[] {"getSlots", "size"}) {
                try {
                    getSlots = storage.getClass().getMethod(candidate);
                    break;
                } catch (NoSuchMethodException ignored) {
                    // 试下一个 / try the next name
                }
            }
            if (getSlots == null) {
                return 0L;
            }
            java.lang.reflect.Method getStack = storage.getClass().getMethod("getStackInSlot", int.class);
            int slots = ((Number) getSlots.invoke(storage)).intValue();
            long extra = Long.MAX_VALUE;
            for (Ingredient ingredient : ingredients) {
                if (ingredient.isEmpty()) {
                    continue;
                }
                int need = 0;
                for (ItemStack inGrid : grid) {
                    if (!inGrid.isEmpty() && ingredient.test(inGrid)) {
                        need++;
                    }
                }
                if (need <= 0) {
                    continue;
                }
                long have = 0L;
                for (int i = 0; i < slots; i++) {
                    ItemStack stored = (ItemStack) getStack.invoke(storage, i);
                    if (stored != null && !stored.isEmpty() && ingredient.test(stored)) {
                        have += stored.getCount();
                    }
                }
                extra = Math.min(extra, have / need);
                if (extra <= 0L) {
                    return 0L;
                }
            }
            if (extra == Long.MAX_VALUE) {
                return 0L;
            }
            DiagLog.lineThrottled("soph.refill." + extra,
                    "[diag] soph refill: extra=" + extra + " grid=" + grid.size());
            return extra;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    /** 不按 Shift 时的批次就是 1（只显示配方一次出几个）。 / Without Shift the batch is one recipe. */
    private static long ultrastacks$batch(long crafts) {
        boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld();
        if (unlocked) {
            return crafts;
        }
        return shiftHeld()
            ? Math.min(Math.max(1, UltraStacksConfig.bulkCraftLimit()), crafts) : 1L;
    }

    /** 精妙铁砧页的合计：走它内层的 {@code PersistableAnvilMenu}，取左/右槽算一批能做多少。 */

    /** 从当前选中的页签里取容器：铁砧页的容器只挂在页签上，不在菜单的容器表里。 */
    private static Object selectedTabContainer(Object screen, String needle) {
        try {
            Object tab = null;
            for (Class<?> c = screen.getClass(); c != null && tab == null; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Field f = c.getDeclaredField("selectedTab");
                    f.setAccessible(true);
                    tab = f.get(screen);
                } catch (NoSuchFieldException ignored) {
                    // 继续往上找 / keep walking up
                }
            }
            if (tab == null) {
                return null;
            }
            for (Class<?> c = tab.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Method m = c.getDeclaredMethod("getContainer");
                    m.setAccessible(true);
                    Object container = m.invoke(tab);
                    if (container != null && container.getClass().getName().contains(needle)) {
                        return container;
                    }
                } catch (NoSuchMethodException ignored) {
                    // 继续往上找 / keep walking up
                }
            }
        } catch (Throwable ignored) {
            // 取不到就当没有 / treat as absent
        }
        return null;
    }


    /** 取选中页签容器自己的槽位（铁砧页的左/右/产物都在这里，外层菜单里没有）。 */
    public static java.util.List<net.minecraft.world.inventory.Slot> sophInnerSlots(AbstractContainerScreen<?> screen, String needle) {
        java.util.List<net.minecraft.world.inventory.Slot> out = new java.util.ArrayList<>();
        try {
            Object container = selectedTabContainer(screen, needle);
            if (container == null) {
                return out;
            }
            java.lang.reflect.Method m = container.getClass().getMethod("getSlots");
            Object slots = m.invoke(container);
            if (slots instanceof java.util.List<?> list) {
                for (Object o : list) {
                    if (o instanceof net.minecraft.world.inventory.Slot slot) {
                        out.add(slot);
                    }
                }
            }
        } catch (Throwable ignored) {
            // 取不到就当没有 / treat as absent
        }
        return out;
    }

    public static long sophAnvilTotal(AbstractContainerScreen<?> screen) {
        try {
            Object container = findUpgradeContainer(screen.getMenu(), "upgrades.anvil.AnvilUpgradeContainer");
            if (container == null) {
                container = selectedTabContainer(screen, "upgrades.anvil.AnvilUpgradeContainer");
            }
            if (container == null) {
                return -1L;
            }
            Object menu = null;
            for (Class<?> c = container.getClass(); c != null && menu == null; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Field f = c.getDeclaredField("anvilMenuDelegate");
                    f.setAccessible(true);
                    menu = f.get(container);
                } catch (NoSuchFieldException ignored) {
                    // 继续往上找 / keep walking up
                }
            }
            if (!(menu instanceof AnvilMenu anvil)) {
                return -1L;
            }
            ItemStack left = anvil.getSlot(0).getItem();
            ItemStack out = anvil.getSlot(2).getItem();
            if (left.isEmpty() || out.isEmpty()) {
                return -1L;
            }
            // 左槽只有一件时 available 就是 1，后面 batch<=1 会直接不画，这里不再提前返回，
            // 否则书本合并（左右各一摞、产物只有 1 件）会被误判成“没什么可预览”。
            // Do not bail out on a single left item: the batch check below already covers it, and an early
            // return here also hid the total for book merging, where the output stack holds a single book.
            long available = left.getCount();
            ItemStack additive = anvil.getSlot(1).getItem();
            if (!additive.isEmpty()) {
                available = Math.min(available, additive.getCount());
            }
            boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld();
            boolean shift = shiftHeld();
            long limit = Math.max(1, UltraStacksConfig.bulkCraftLimit());
            // 精妙铁砧这一格显示「这个配方最多能做多少」，不跟 Shift 走：它和原版铁砧不同，
            // 原版一次只修一件、要按 Shift 才批量；精妙这边用户要的是「能做多少」这个信息。
            // This cell reports how many the recipe can still do, independent of Shift: unlike the vanilla
            // anvil (one item per take, Shift to batch), the number here is the remaining capacity.
            long batch = unlocked ? available : (shift ? Math.min(limit, available) : 1L);
            if (batch <= 1L) {
                return -1L;
            }
            return out.getCount() * batch;
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    /** 精妙合成页的产物槽（反射取），没有则 null。 */
    static Slot sophCraftingResultSlot(AbstractContainerScreen<?> screen) {
        try {
            Object container = findUpgradeContainer(screen.getMenu(), "upgrades.crafting.CraftingUpgradeContainer");
            if (container == null) {
                return null;
            }
            for (Class<?> c = container.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Field f = c.getDeclaredField("craftingResultSlot");
                    f.setAccessible(true);
                    return f.get(container) instanceof Slot slot ? slot : null;
                } catch (NoSuchFieldException ignored) {
                    // 继续往上找 / keep walking up
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return null;
    }

    private static Object findUpgradeContainer(AbstractContainerMenu menu, String classNamePart) throws Exception {
        java.lang.reflect.Field containersField = null;
        for (Class<?> c = menu.getClass(); c != null && containersField == null; c = c.getSuperclass()) {
            try {
                containersField = c.getDeclaredField("upgradeContainers");
            } catch (NoSuchFieldException ignored) {
                // 继续往上找 / keep walking up
            }
        }
        if (containersField == null) {
            return null;
        }
        containersField.setAccessible(true);
        if (!(containersField.get(menu) instanceof java.util.Map<?, ?> containers)) {
            return null;
        }
        for (Object container : containers.values()) {
            if (container.getClass().getName().contains(classNamePart)) {
                return container;
            }
        }
        return null;
    }
    /**
     * 精妙铁砧页专用入口：外层菜单里没有那三个槽，必须拿内层菜单的 0/1/2 号槽自己算。
     *
     * <p>Entry point for the upgrade tab: its three anvil slots are not part of the outer menu, so the
     * inner menu's slots 0/1/2 are used directly.
     */
    public static void drawSophisticatedAnvil(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen) {
        if (!UltraStacksConfig.displayModule() || screen.getMenu() == null) {
            return;
        }
        Slot[] inputs = sophisticatedAnvilInputs(screen.getMenu());
        if (inputs == null) {
            return;
        }
        AbstractContainerMenu inner = inputsInnerMenu;
        if (inner == null || inner.slots.size() < 3) {
            return;
        }
        // 精妙升级页的槽位坐标是相对页面的，物品本身画在 (guiLeft + slot.x, guiTop + slot.y)；
        // 只有它那圈背景框会再往左上各偏 1 像素（renderSlotBg 里是 guiLeft-1）。
        // The tab stores slot coordinates relative to the page: the icon itself sits at
        // (guiLeft + slot.x, guiTop + slot.y), and only its background frame is shifted by -1.
        drawAnvilResult(graphics, inner.getSlot(2), inputs[0], inputs[1],
            screen.getGuiLeft(), screen.getGuiTop());
    }

    /**
     * 精妙铁砧页的产物槽：{@link net.minecraft.world.inventory.ResultSlot} 且构造下标为 2，界面属于精妙。
     *
     * <p>The anvil tab's result slot: a {@code ResultSlot} built with index 2 inside a Sophisticated screen.
     */
    private static boolean isSophisticatedAnvilResult(AbstractContainerScreen<?> screen, Slot slot) {
        return slot instanceof net.minecraft.world.inventory.ResultSlot
                && slot.index == 2
                && screen.getClass().getName().startsWith("net.p3pp3rf1y.");
    }
    /**
     * 铁砧产物预览：按左/右槽剩余材料算这一批能做多少。
     *
     * <p>The anvil preview: how many this batch can produce given the two input slots.
     */
    private static void drawAnvilResult(GuiGraphicsExtractor graphics, Slot result, Slot leftSlot, Slot rightSlot, int dx, int dy) {
        ItemStack out = result.getItem();
        if (out.isEmpty()) {
            return;
        }
        ItemStack left = leftSlot.getItem();
        if (left.isEmpty() || left.getCount() <= 1) {
            return;
        }
        long available = left.getCount();
        ItemStack additive = rightSlot == null ? ItemStack.EMPTY : rightSlot.getItem();
        if (!additive.isEmpty()) {
            available = Math.min(available, additive.getCount());
        }
        boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld();
        boolean shift = shiftHeld();
        long limit = Math.max(1, UltraStacksConfig.bulkCraftLimit());
        long batch = unlocked ? available : (shift ? Math.min(limit, available) : 1);
        drawTotal(graphics, result, out, batch, dx, dy);
    }
    /**
     * 把「一份 × 批数」画到产物格上，只有比原有数量大才画。
     *
     * <p>Draws {@code perCraft × batch} on the result slot, only when it exceeds the plain count.
     */
    private static void drawTotal(GuiGraphicsExtractor graphics, Slot slot, ItemStack out, long batch, int dx, int dy) {
        drawTotal(graphics, slot, out, batch, dx, dy, out.getCount());
    }

    private static void drawTotal(GuiGraphicsExtractor graphics, Slot slot, ItemStack out, long batch, int dx, int dy,
                                  int perCraft) {
        long total = (long) perCraft * batch;
        // 只有「这一批比格子里的那点东西更多」才值得画。精妙铁砧单件重命名时 total 正好等于 1，
        // 画出来就是那个莫名其妙的 1；原版本来就只画多于一件的数量，这里对齐同一规则。
        // Only draw when the batch is actually more than what the slot already shows. A single rename
        // makes total == 1, which is exactly the stray "1" people see; vanilla never prints a one either.
        if (total <= 0L || total <= out.getCount()) {
            return;
        }
        // 抬到 300：原版装饰在 200、提示框在 400，这样预览不会被格子里的文字或图标盖住。
        // Lift to z=300 so the preview sits above vanilla decorations (200) but below tooltips (400).
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(0.0F, 0.0F);
        lastDrawnStack = out.copyWithCount(1);
        CountRenderer.drawForSlot(graphics, Minecraft.getInstance().font, CountFormat.format(total), slot.x + dx, slot.y + dy);
        pose.popMatrix();
        rememberCell(slot.x + dx, slot.y + dy, total);
        // 精妙升级页在它自己的平移里直接拿 slot.x/slot.y 画，同一个格子要按两种坐标系各记一份。
        // The upgrade tab draws inside its own translation using slot.x/slot.y, so both coordinate
        // systems are remembered for the same cell.
        rememberCell(slot.x, slot.y, total);
    }
    /**
     * 精妙铁砧页的两个输入槽不在外层菜单里（它们属于内层 {@code PersistableAnvilMenu}），
     * 只能顺着菜单里的升级容器反射到内层菜单再取 0/1 号槽。
     *
     * <p>The two input slots live in the tab's inner {@code PersistableAnvilMenu}, not in the outer menu,
     * so they are reached by walking the menu's upgrade containers reflectively.
     */
    private static AbstractContainerMenu inputsInnerMenu;
    private static Slot[] sophisticatedAnvilInputs(AbstractContainerMenu menu) {
        try {
            java.lang.reflect.Field containersField = null;
            for (Class<?> c = menu.getClass(); c != null && containersField == null; c = c.getSuperclass()) {
                try {
                    containersField = c.getDeclaredField("upgradeContainers");
                } catch (NoSuchFieldException ignored) {
                    // 继续往上找 / keep walking up
                }
            }
            if (containersField == null) {
                return null;
            }
            containersField.setAccessible(true);
            Object value = containersField.get(menu);
            if (!(value instanceof java.util.Map<?, ?> containers)) {
                return null;
            }
            for (Object container : containers.values()) {
                if (!container.getClass().getName().contains("upgrades.anvil.AnvilUpgradeContainer")) {
                    continue;
                }
                java.lang.reflect.Field delegateField = container.getClass().getDeclaredField("anvilMenuDelegate");
                delegateField.setAccessible(true);
                Object delegate = delegateField.get(container);
                if (!(delegate instanceof AbstractContainerMenu inner) || inner.slots.size() < 2) {
                    return null;
                }
                inputsInnerMenu = inner;
                Slot first = inner.getSlot(0);
                Slot second = inner.getSlot(1);
                return new Slot[] {first, second};
            }
        } catch (Throwable ignored) {
            // 反射失败就当拿不到，宁可不画 / reflection failure means no preview
        }
        return null;
    }
    /**
     * 从菜单里找回铁砧的两个输入槽（容器是 2 格 {@code SimpleContainer}），按容器下标排好。
     *
     * <p>Finds the two anvil input slots (a two-slot {@code SimpleContainer}) and orders them by index.
     */
    private static Slot[] anvilInputs(AbstractContainerMenu menu) {
        Slot first = null;
        Slot second = null;
        for (Slot candidate : menu.slots) {
            if (!(candidate.container instanceof net.minecraft.world.SimpleContainer simple)
                    || simple.getContainerSize() != 2) {
                continue;
            }
            if (candidate.getContainerSlot() == 0) {
                first = candidate;
            } else if (candidate.getContainerSlot() == 1) {
                second = candidate;
            }
        }
        return first == null ? null : new Slot[] {first, second};
    }
}
