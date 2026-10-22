package com.zaixiayesheng.ultrastacks;

import com.zaixiayesheng.ultrastacks.core.StackLimits;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Core config for 1.0.0: three keys only.
 *
 * <p>Helpers below are called from the mixins; they intentionally stay tiny and side-effect free
 * so the behaviour lives in one place.
 *
 * <p><b>中文</b> 三项配置键，以及与 mixin 共用的纯函数；行为决策集中在这里。
 */
public final class UltraStacksConfig {
    public static final int DEFAULT_MAX_STACK_SIZE = 65536;
    public static final int SAFE_MAX_STACK_SIZE = 65536;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAX_STACK_SIZE = BUILDER
            .comment("Stack limit for stackable items (vanilla 64-size items).",
                    "Range: 64 - " + UltraStacks.ABSOLUTE_MAX_STACK_SIZE)
            .translation("ultrastacks.configuration.maxStackSize")
            .defineInRange("maxStackSize", DEFAULT_MAX_STACK_SIZE, 64, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    private static final ModConfigSpec.BooleanValue SCALE_SMALL_ITEMS = BUILDER
            .comment("Scale smaller vanilla stacks proportionally (16-size items become a quarter of maxStackSize).")
            .translation("ultrastacks.configuration.scaleSmallItems")
            .define("scaleSmallItems", true);

    private static final ModConfigSpec.BooleanValue EXPERIMENTAL = BUILDER
            .comment("Allow maxStackSize above " + SAFE_MAX_STACK_SIZE + ".",
                    "Experimental: mods that assume small stacks may misbehave or lose items.")
            .translation("ultrastacks.configuration.experimentalStackSizes")
            .define("experimentalStackSizes", false);

    private static final ModConfigSpec.BooleanValue CRAFTING_MODULE = BUILDER
            .comment("Crafting module: one shift-click finishes a whole batch.",
                    "中文：合成模块：一次 Shift 点击把整批合完。")
            .translation("ultrastacks.configuration.crafting")
            .define("crafting", true);

    private static final ModConfigSpec.BooleanValue DISPLAY_MODULE = BUILDER
            .comment("Display module: stack counts drawn in the GUI.",
                    "中文：显示模块：界面上的堆叠数量。")
            .translation("ultrastacks.configuration.display")
            .define("display", true);

    private static final ModConfigSpec.BooleanValue BULK_CRAFTING = BUILDER
            .comment("Crafting module: bulk crafting. Off = vanilla one craft per click.",
                    "中文：合成模块：批量模式；关闭后恢复原版一次一件。")
            .translation("ultrastacks.configuration.bulkCrafting")
            .define("bulkCrafting", true);

    /** How the display module prints a count. */
    public enum CountStyle {
        /** Abbreviate large numbers (1.2k / 3.4m). */
        COMPACT,
        /** Print every digit. */
        EXACT
    }

    private static final ModConfigSpec.EnumValue<CountStyle> COUNT_STYLE = BUILDER
            .comment("Display module: COMPACT abbreviates large counts (12k), EXACT prints every digit.",
                    "中文：显示模块：COMPACT 缩写（12k），EXACT 完整数字。")
            .translation("ultrastacks.configuration.countStyle")
            .defineEnum("countStyle", CountStyle.COMPACT);

    private static final ModConfigSpec.IntValue ABBREVIATE_ABOVE = BUILDER
            .comment("Display module: with COMPACT, counts from this value up are abbreviated.",
                    "中文：显示模块：COMPACT 模式下，达到该数量即缩写。")
            .translation("ultrastacks.configuration.abbreviateAbove")
            .defineInRange("abbreviateAbove", 10000, 256, 1_000_000_000);

    private static final ModConfigSpec.BooleanValue TOOLTIP_EXACT = BUILDER
            .comment("Display module: add the exact count and the stack limit to the item tooltip.",
                    "中文：显示模块：在物品提示框里显示准确数量与堆叠上限。")
            .translation("ultrastacks.configuration.tooltipExact")
            .define("tooltipExact", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile int maxStackSize = DEFAULT_MAX_STACK_SIZE;
    private static volatile boolean scaleSmallItems = true;
    private static volatile boolean craftingModule = true;
    private static volatile boolean displayModule = true;
    private static volatile boolean bulkCrafting = true;
    private static volatile CountStyle countStyle = CountStyle.COMPACT;
    private static volatile int abbreviateAbove = 10000;
    private static volatile boolean tooltipExact = true;

    private UltraStacksConfig() {
    }

    public static int maxStackSize() {
        return maxStackSize;
    }

    public static boolean scaleSmallItems() {
        return scaleSmallItems;
    }

    public static boolean craftingModule() {
        return craftingModule;
    }

    public static boolean displayModule() {
        return displayModule;
    }

    public static boolean bulkCrafting() {
        return bulkCrafting;
    }

    public static CountStyle countStyle() {
        return countStyle;
    }

    public static int abbreviateAbove() {
        return abbreviateAbove;
    }

    public static boolean tooltipExact() {
        return tooltipExact;
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        int requested = Math.min(MAX_STACK_SIZE.get(), UltraStacks.ABSOLUTE_MAX_STACK_SIZE);
        scaleSmallItems = SCALE_SMALL_ITEMS.get();
        craftingModule = CRAFTING_MODULE.get();
        displayModule = DISPLAY_MODULE.get();
        bulkCrafting = BULK_CRAFTING.get();
        countStyle = COUNT_STYLE.get();
        abbreviateAbove = ABBREVIATE_ABOVE.get();
        tooltipExact = TOOLTIP_EXACT.get();
        if (requested > SAFE_MAX_STACK_SIZE && !EXPERIMENTAL.get()) {
            UltraStacks.LOGGER.warn("maxStackSize {} is above the safe value {}; using the safe value. "
                    + "Enable experimentalStackSizes to allow it.", requested, SAFE_MAX_STACK_SIZE);
            requested = SAFE_MAX_STACK_SIZE;
        }

        if (requested != maxStackSize) {
            UltraStacks.LOGGER.info("maxStackSize: {} -> {}", maxStackSize, requested);
            maxStackSize = requested;
        }
    }

    /** Vanilla containers default to 99; walk that placeholder up to the configured limit. */
    public static int raiseVanillaDefault(int original) {
        return original == 99 ? maxStackSize : original;
    }

    /**
     * Slot limits: vanilla reporting 64 or 99 means "the default container size", which the caller
     * really wants raised. Slots that legitimately hold one item (potions, tools) return 1 and stay.
     */
    public static int raiseSlotLimit(int original) {
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /** Slot limits reported by {@code ItemStackHandler} and friends; never lower, only raise. */
    public static int raiseModdedSlotLimit(int original) {
        return Math.max(original, maxStackSize);
    }

    /** Codec bound for saved/networked counts: a constant, see {@link UltraStacks#ABSOLUTE_MAX_STACK_SIZE}. */
    public static int codecUpperBound() {
        return UltraStacks.ABSOLUTE_MAX_STACK_SIZE;
    }

    /** Replace vanilla's {@code ExtraCodecs.intRange(1, 99)} upper bound only. */
    public static int raiseCodecUpperBound(int bound) {
        return bound == 99 ? codecUpperBound() : bound;
    }

    /** New stack limit for an item whose vanilla limit is {@code vanillaMax}. */
    public static int scaledMax(int vanillaMax) {
        return StackLimits.resolve(vanillaMax, maxStackSize, scaleSmallItems);
    }

    /**
     * Stack limit reported for {@code stack}. Damageable items and single-stack items keep their vanilla
     * limit; everything else follows {@link #scaledMax(int)}.
     *
     * <p>Item components are still unbound while the game registers items, so a failure to inspect the
     * stack falls back to the vanilla value instead of throwing.
     */
    public static int maxStackSizeFor(ItemStack stack, int vanillaMax) {
        if (vanillaMax <= 1) {
            return vanillaMax;
        }
        try {
            if (stack.isDamageableItem()) {
                return vanillaMax;
            }
        } catch (RuntimeException ignored) {
            return vanillaMax;
        }
        return Math.max(vanillaMax, scaledMax(vanillaMax));
    }
}
