package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.DiagLog;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
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
 *
 * <p>1.20.1 没有 {@code CraftingInput}，配方查询走 {@link CraftingContainer}：这里用一个只读的九格容器
 * 承载格子里的物品，再交给 {@code RecipeManager#getRecipeFor}。
 */
public final class ResultPreview {
    private ResultPreview() {
    }

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
        int gx = (int) Math.round(mc.mouseHandler.xpos() * guiW / winW);
        int gy = (int) Math.round(mc.mouseHandler.ypos() * guiH / winH);
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

    /**
     * 鼠标底下那一格我们画了多少（精妙的槽位不在菜单里，只能按屏幕坐标反查）。
     *
     * <p>How much our preview drew under the cursor: the upgrade tabs keep their slots outside the
     * menu, so the cell is looked up by screen position instead.
     */
    public static long totalUnderMouse(AbstractContainerScreen<?> screen) {
        var mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return -1L;
        }
        double guiW = mc.getWindow().getGuiScaledWidth();
        double guiH = mc.getWindow().getGuiScaledHeight();
        double winW = Math.max(1, mc.getWindow().getScreenWidth());
        double winH = Math.max(1, mc.getWindow().getScreenHeight());
        int gx = (int) Math.round(mc.mouseHandler.xpos() * guiW / winW);
        int gy = (int) Math.round(mc.mouseHandler.ypos() * guiH / winH);
        return drawnTotalAt(gx, gy);
    }

    /**
     * 找出当前 {@link GuiGraphics} 是否已经平移到了界面原点。
     *
     * <p>在原版 {@code renderSlot} 里，绘制时姿态已经平移过 {@code guiLeft/guiTop}，此时槽位坐标
     * 直接可用；AE 与精妙这些自己画界面的地方是在未平移的姿态里调用，需要自己补上界面原点。
     */
    static int[] origin(GuiGraphics graphics, AbstractContainerScreen<?> screen) {
        var matrix = graphics.pose().last().pose();
        int tx = Math.round(matrix.m30());
        int ty = Math.round(matrix.m31());
        if (tx == 0 && ty == 0) {
            return new int[] {screen.getGuiLeft(), screen.getGuiTop()};
        }
        return new int[] {0, 0};
    }

    public static void draw(GuiGraphics graphics, AbstractContainerScreen<?> screen, Slot slot) {
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
        // 按「精妙界面 + 第 2 号槽」认定；类型卡死会让提示框永远拿不到合计。
        // The anvil result slot inside Sophisticated is not always a vanilla ResultSlot, so match on
        // "Sophisticated screen + slot index 2" instead of the class.
        if (screen.getClass().getName().startsWith("net.p3pp3rf1y.") && isSophAnvilResult(screen, slot)) {
            long total = sophAnvilTotal(screen);
            if (total > 0L) {
                return total;
            }
        }
        if (screen.getMenu() instanceof appeng.menu.me.common.MEStorageMenu) {
            // AE 只认产物格那一格，别把合计行带到别的物品上。
            // AE only reports the crafting result, so the batch line never leaks onto other stacks.
            // 只认 AE 自己的产物槽：列表里同名的木板格不能借用这串数字，否则它的真实数量会被盖掉。
            // Only AE's own result slot may borrow this number; a same-named plank cell in the grid
            // list must keep its own count.
            return slot instanceof appeng.menu.slot.CraftingTermSlot
                    ? AeCraftPreview.previewTotal(slot)
                    : -1L;
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
        boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        long limit = Math.max(1, UltraStacksConfig.bulkCraftLimit());
        if (slot != null && slot == sophCraftingResultSlot(screen)) {
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
        List<ItemStack> grid = gridStacks(menu);
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return -1L;
        }
        CraftingRecipe recipe = recipeFor(grid, level);
        if (recipe == null) {
            return -1L;
        }
        long crafts = Math.max(1L, maxCrafts(recipe.getIngredients(), grid));
        long batch = unlocked ? crafts : (shift ? Math.min(Math.max(1, limit), crafts) : 1L);
        // 预览要显式乘上配方产出：木头 1 根出 4 块板，按 Shift 就应该是 4 × 64，而不是 64。
        // Explicitly multiply by the recipe output: one log makes four planks, so Shift shows 4 × 64.
        int perCraft = Math.max(1, recipe.getResultItem(level.registryAccess()).getCount());
        return (long) perCraft * batch;
    }

    /**
     * 这一格是不是「真正的产物槽」——精妙背包的物品格也会出现第 3 个容器槽号，只按槽号认会把材料当产物。
     *
     * <p>Whether this slot really is a result slot: backpack storage slots also use small container
     * indices, so index alone would paint numbers onto raw materials.
     */
    public static boolean isResultSlotFor(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen,
            Slot slot) {
        if (slot == null) {
            return false;
        }
        if ("appeng.menu.slot.CraftingTermSlot".equals(slot.getClass().getName())) {
            return true;
        }
        if (screen != null) {
            String name = screen.getClass().getName();
            if (name.startsWith("net.p3pp3rf1y.")) {
                return slot == sophCraftingResultSlot(screen) || isSophAnvilResult(screen, slot);
            }
        }
        return slot.getContainerSlot() == 2
                && !(slot.container instanceof net.minecraft.world.entity.player.Inventory);
    }

    /** 精妙铁砧的产物槽：容器是不是那个可持久化铁砧菜单，槽号 2。 */
    private static boolean isSophAnvilResult(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen,
            Slot slot) {
        return slot != null && slot.getContainerSlot() == 2
                && slot.container.getClass().getName().contains("PersistableAnvilMenu");
    }

    /** 精妙界面里按鼠标坐标反查这一格，命中就返回我们画过的合计。 */
    public static long previewTotalUnderMouse(AbstractContainerScreen<?> screen) {
        return UltraStacksConfig.displayModule() ? totalUnderMouse(screen) : -1L;
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

    /**
     * 用九格物品查配方。1.20.1 没有 {@code CraftingInput}，这里用一个只读容器承载它们。
     *
     * <p>Looks the recipe up from nine grid stacks; 1.20.1 has no {@code CraftingInput}, so a read-only
     * container carries them.
     */
    static CraftingRecipe recipeFor(List<ItemStack> grid, net.minecraft.world.level.Level level) {
        if (grid.size() != 9 || level == null) {
            return null;
        }
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, new GridContainer(grid), level)
                .orElse(null);
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
        for (ItemStack stack : ingredient.getItems()) {
            builder.append(stack.getItem().toString()).append(',');
        }
        return builder.toString();
    }

    /**
     * 精妙背包/存储的合成升级页：结果槽和九格配方格都在内层容器里，按配方与库存算一批能做多少。
     *
     * <p>The crafting upgrade tab keeps its result slot and 3×3 grid inside the upgrade container, so the
     * batch size is derived from the recipe plus whatever the menu exposes as ingredients.
     */
    public static void drawSophisticatedCrafting(GuiGraphics graphics, AbstractContainerScreen<?> screen) {
        if (!UltraStacksConfig.displayModule() || screen.getMenu() == null) {
            return;
        }
        try {
            Object container = findUpgradeContainer(screen.getMenu(), "upgrades.crafting.CraftingUpgradeContainer");
            if (container == null) {
                return;
            }
            Slot result = craftingResultSlot(container);
            if (result == null) {
                return;
            }
            ItemStack out = result.getItem();
            if (out.isEmpty()) {
                return;
            }
            List<ItemStack> grid = recipeGrid(container);
            if (grid == null) {
                return;
            }
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            CraftingRecipe recipe = recipeFor(grid, level);
            if (recipe == null) {
                return;
            }
            long crafts = maxCrafts(recipe.getIngredients(), grid);
            if (crafts <= 1) {
                return;
            }
            // 精妙产物槽里常常只摆一个，真正的“一次给多少”要看配方本身。
            // The upgrade tab shows a single sample, so one craft's output comes from the recipe.
            int perCraft = Math.max(1, recipe.getResultItem(level.registryAccess()).getCount());
            SOPH_PER_CRAFT.put(screen, perCraft);
            long batch = ultrastacks$batch(crafts);
            DiagLog.lineThrottled("soph.craft." + batch,
                    "[diag] soph craft: crafts=" + crafts + " perCraft=" + perCraft + " batch=" + batch
                            + " slotCount=" + out.getCount() + " total=" + (perCraft * batch)
                            + " slot=" + result.x + "," + result.y + " gui=" + screen.getGuiLeft() + "," + screen.getGuiTop());
            drawTotal(graphics, result, out, batch, screen.getGuiLeft(), screen.getGuiTop(), perCraft);
        } catch (Throwable ignored) {
            // 反射失败就当拿不到 / reflection failure means no preview
        }
    }

    /** 记忆每种精妙合成页配方一次的产出数：合成完那一帧配方/材料可能暂时读不到，仍要显示「这个配方一次出几个」。
     *  Remembers the per-craft output of each Sophisticated crafting screen: right after a craft the recipe or the
     *  grid can be momentarily unreadable, yet the recipe's own output count must still be shown. */
    private static final java.util.Map<AbstractContainerScreen<?>, Integer> SOPH_PER_CRAFT =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * 精妙合成升级页这一批能做多少（含配方产出数量）；拿不到返回 -1。
     *
     * <p>How much one batch makes on the Sophisticated crafting tab, recipe output included; -1 when
     * the tab cannot be resolved.
     */
    public static long sophCraftingTotal(AbstractContainerScreen<?> screen) {
        try {
            Object container = findUpgradeContainer(screen.getMenu(), "upgrades.crafting.CraftingUpgradeContainer");
            if (container == null) {
                DiagLog.lineThrottled("soph.total.nocontainer",
                        "[diag] soph total: no CraftingUpgradeContainer, menu=" + screen.getMenu().getClass().getName());
                return -1L;
            }
            Slot result = craftingResultSlot(container);
            if (result == null || result.getItem().isEmpty()) {
                Integer cached = SOPH_PER_CRAFT.get(screen);
                return cached == null ? -1L : cached.longValue() * ultrastacks$batch(1);
            }
            List<ItemStack> grid = recipeGrid(container);
            if (grid == null) {
                return -1L;
            }
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return -1L;
            }
            CraftingRecipe recipe = recipeFor(grid, level);
            if (recipe == null) {
                return -1L;
            }
            long crafts = maxCrafts(recipe.getIngredients(), grid);
            if (crafts <= 0) {
                Integer cached = SOPH_PER_CRAFT.get(screen);
                return cached == null ? -1L : cached.longValue() * ultrastacks$batch(1);
            }
            int perCraft = Math.max(1, recipe.getResultItem(level.registryAccess()).getCount());
            // 开了“自动补货”时，格子里的材料会被背包里的同类物品续上，预览要按续得上多少算，
            // 否则永远只显示一次的量。
            // With auto-refill on, the grid keeps being topped up from the backpack, so the preview has
            // to count what can actually be refilled instead of a single craft.
            // 只加上「续补充得起多少次」，不再拿堆叠上限去夹——上限是显示分母，不是能合成多少。
            // Only add what refill can actually top up; the stack limit is the denominator on screen,
            // not a bound on how much can be crafted.
            crafts = crafts + refillCrafts(container, screen.getMenu(), recipe, grid);
            long batch = ultrastacks$batch(crafts);
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
     * <p>How many extra crafts auto-refill can top up: each ingredient's supply in the backpack divided
     * by how many of it one craft eats, the tightest one wins. Returns 0 when refill is off.
     */
    private static long refillCrafts(Object container, AbstractContainerMenu menu, CraftingRecipe recipe,
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
            java.lang.reflect.Method getSlots = storage.getClass().getMethod("getSlots");
            java.lang.reflect.Method getStack = storage.getClass().getMethod("getStackInSlot", int.class);
            int slots = (int) getSlots.invoke(storage);
            long extra = Long.MAX_VALUE;
            for (Ingredient ingredient : recipe.getIngredients()) {
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
                    if (!stored.isEmpty() && ingredient.test(stored)) {
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
        return net.minecraft.client.gui.screens.Screen.hasShiftDown()
            ? Math.min(Math.max(1, UltraStacksConfig.bulkCraftLimit()), crafts) : 1L;
    }

    /** 反射取精妙合成页容器里的产物槽，拿不到返回 null。 */
    private static Slot craftingResultSlot(Object container) {
        for (Class<?> c = container.getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField("craftingResultSlot");
                f.setAccessible(true);
                return f.get(container) instanceof Slot slot ? slot : null;
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                // 继续往上找 / keep walking up
            }
        }
        return null;
    }

    /** 反射取精妙合成页容器的九格配方槽，返回九个物品副本；拿不到返回 null。 */
    private static List<ItemStack> recipeGrid(Object container) {
        try {
            java.lang.reflect.Method slotsMethod = container.getClass().getMethod("getRecipeSlots");
            if (!(slotsMethod.invoke(container) instanceof java.util.List<?> slots)) {
                return null;
            }
            List<ItemStack> grid = new ArrayList<>(9);
            for (Object element : slots) {
                if (element instanceof Slot slot) {
                    grid.add(slot.getItem().copy());
                }
            }
            return grid.size() == 9 ? grid : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

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
    public static java.util.List<net.minecraft.world.inventory.Slot> sophInnerSlots(
            AbstractContainerScreen<?> screen, String needle) {
        java.util.List<net.minecraft.world.inventory.Slot> out = new java.util.ArrayList<>();
        try {
            Object container = selectedTabContainer(screen, needle);
            if (container == null) {
                return out;
            }
            java.lang.reflect.Method m = container.getClass().getMethod("getSlots");
            if (m.invoke(container) instanceof java.util.List<?> list) {
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

    /** 精妙铁砧页的合计：走它内层的 {@code PersistableAnvilMenu}，取左/右槽算一批能做多少。 */
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
            long limit = Math.max(1, UltraStacksConfig.bulkCraftLimit());
            // 与其它产物格同一套口径：不按 Shift 只算一次，按住按上限，解锁按余量。
            // Same rule as every other result slot: one per take, Shift up to the limit, unlock for the rest.
            boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
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
            return container == null ? null : craftingResultSlot(container);
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
    public static void drawSophisticatedAnvil(GuiGraphics graphics, AbstractContainerScreen<?> screen) {
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

    /** 铁砧产物预览：按左/右槽剩余材料算这一批能做多少。 */
    private static void drawAnvilResult(GuiGraphics graphics, Slot result, Slot leftSlot, Slot rightSlot,
            int dx, int dy) {
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
        long batch = ultrastacks$batch(available);
        drawTotal(graphics, result, out, batch, dx, dy);
    }

    /**
     * 把「一份 × 批数」画到产物格上，只有比原有数量大才画。
     *
     * <p>Draws {@code perCraft × batch} on the result slot, only when it exceeds the plain count.
     */
    private static void drawTotal(GuiGraphics graphics, Slot slot, ItemStack out, long batch, int dx, int dy) {
        drawTotal(graphics, slot, out, batch, dx, dy, out.getCount());
    }

    private static void drawTotal(GuiGraphics graphics, Slot slot, ItemStack out, long batch, int dx, int dy,
                                  int perCraft) {
        long total = (long) perCraft * batch;
        // 只有「这一批比格子里的那点东西更多」才值得画。精妙铁砧单件重命名时 total 正好等于 1，
        // 画出来就是那个莫名其妙的 1；原版本来就只画多于一件的数量，这里对齐同一规则。
        // Only draw when the batch is actually more than what the slot already shows. A single rename
        // makes total == 1, which is exactly the stray "1" people see; vanilla never prints a one either.
        // 只要比「单个一种」多就画：产物数量恰好等于槽里那点时（木头 1 根出 4 块板）也要画，
        // 否则屏幕上只剩原版那个数字，看起来像没生效。单个的 1 仍然不画。
        // Draw whenever the batch is more than a single item, including when the total equals what the
        // slot already shows; a lone "1" is still skipped.
        if (total <= 1L || total < out.getCount()) {
            return;
        }
        // 抬到 300：原版装饰在 200、提示框在 400，这样预览不会被格子里的文字或图标盖住。
        // Lift to z=300 so the preview sits above vanilla decorations (200) but below tooltips (400).
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 300.0F);
        lastDrawnStack = out.copyWithCount(1);
        CountRenderer.drawForSlot(graphics, Minecraft.getInstance().font, CountFormat.format(total),
                slot.x + dx, slot.y + dy);
        pose.popPose();
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
                return new Slot[] {inner.getSlot(0), inner.getSlot(1)};
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

    /**
     * 只读九格容器：把格子里的物品交给配方查询，不碰任何菜单回调。
     *
     * <p>Read-only nine-slot container used only for the recipe lookup, so no menu callback fires
     * while rendering.
     */
    private static final class GridContainer implements CraftingContainer {
        private final List<ItemStack> items;

        GridContainer(List<ItemStack> items) {
            this.items = items;
        }

        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public List<ItemStack> getItems() {
            return items;
        }

        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < items.size() ? items.get(index) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            // 只读 / read-only
        }

        @Override
        public void setChanged() {
            // 只读 / read-only
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            // 只读 / read-only
        }

        @Override
        public void fillStackedContents(net.minecraft.world.entity.player.StackedContents contents) {
            for (ItemStack stack : items) {
                contents.accountSimpleStack(stack);
            }
        }
    }
}
