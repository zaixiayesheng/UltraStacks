package com.zaixiayesheng.ultrastacks;

import com.zaixiayesheng.ultrastacks.core.StackLimits;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Core config for 1.0.0: three keys only.
 *
 * <p><b>中文</b> 三项配置键，以及与 mixin 共用的纯函数；行为决策集中在这里。
 */
@Mod.EventBusSubscriber(modid = UltraStacks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class UltraStacksConfig {
    public static final int DEFAULT_MAX_STACK_SIZE = 65536;
    public static final int SAFE_MAX_STACK_SIZE = 65536;

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue MAX_STACK_SIZE = BUILDER
            .comment("Stack limit for stackable items (vanilla 64-size items).",
                    "Range: 64 - " + UltraStacks.ABSOLUTE_MAX_STACK_SIZE)
            .translation("ultrastacks.configuration.maxStackSize")
            .defineInRange("maxStackSize", DEFAULT_MAX_STACK_SIZE, 64, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    private static final ForgeConfigSpec.BooleanValue SCALE_SMALL_ITEMS = BUILDER
            .comment("Scale smaller vanilla stacks proportionally (16-size items become a quarter of maxStackSize).")
            .translation("ultrastacks.configuration.scaleSmallItems")
            .define("scaleSmallItems", true);

    private static final ForgeConfigSpec.BooleanValue EXPERIMENTAL = BUILDER
            .comment("Allow maxStackSize above " + SAFE_MAX_STACK_SIZE + ".",
                    "Experimental: mods that assume small stacks may misbehave or lose items.")
            .translation("ultrastacks.configuration.experimentalStackSizes")
            .define("experimentalStackSizes", false);

    private static final ForgeConfigSpec.IntValue POTION_STACK_LIMIT = BUILDER
            .comment("药水每堆最多几个（药水、喷溅、滞留、水瓶）。",
            "0 = Auto（自动）：堆叠上限的 1/16，默认 65536 时为 4096。",
            "1 = 原版（不可堆叠）；其它正数按填写的值生效。",
            "Potions only (potion, splash, lingering, water bottle). 0 = Auto, 1 = vanilla.")
        .translation("ultrastacks.configuration.potionStackLimit")
            .defineInRange("potionStackLimit", 0, 0, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    private static final ForgeConfigSpec.IntValue ENCHANTED_BOOK_STACK_LIMIT = BUILDER
            .comment("附魔书每堆最多几个。",
            "0 = Auto（自动）：堆叠上限的 1/16，默认 65536 时为 4096。",
            "1 = 原版（不可堆叠）；其它正数按填写的值生效。",
            "Enchanted books only. 0 = Auto, 1 = vanilla.")
        .translation("ultrastacks.configuration.enchantedBookStackLimit")
            .defineInRange("enchantedBookStackLimit", 0, 0, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);
    private static final ForgeConfigSpec.IntValue BUCKET_STACK_LIMIT = BUILDER
            .comment("各种桶的堆叠上限；低于空桶上限时自动抬到空桶。",
                    "Bucket stack limit; values below the empty bucket limit are raised to it.")
            .translation("ultrastacks.configuration.bucketStackLimit")
            .defineInRange("bucketStackLimit", 16, 1, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    private static final ForgeConfigSpec.IntValue BREWING_YIELD = BUILDER
            .comment("每个原料能酿出几瓶（原版为 3）。",
                    "Bottles one reagent converts into when brewing stacked potions; vanilla is 3.")
            .translation("ultrastacks.configuration.brewingYield")
            .defineInRange("brewingYield", 3, 1, 4096);

    private static final ForgeConfigSpec.BooleanValue CRAFTING_MODULE = BUILDER
            .comment("Crafting module: one shift-click finishes a whole batch.",
                    "中文：合成模块：一次 Shift 点击把整批合完。")
            .translation("ultrastacks.configuration.crafting")
            .define("crafting", true);

    private static final ForgeConfigSpec.BooleanValue DISPLAY_MODULE = BUILDER
            .comment("Display module: stack counts drawn in the GUI.",
                    "中文：显示模块：界面上的堆叠数量。")
            .translation("ultrastacks.configuration.display")
            .define("display", true);

    private static final ForgeConfigSpec.BooleanValue BULK_CRAFTING = BUILDER
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

    private static final ForgeConfigSpec.EnumValue<CountStyle> COUNT_STYLE = BUILDER
            .comment("Display module: COMPACT abbreviates large counts (12k), EXACT prints every digit.",
                    "中文：显示模块：COMPACT 缩写（12k），EXACT 完整数字。")
            .translation("ultrastacks.configuration.countStyle")
            .defineEnum("countStyle", CountStyle.COMPACT);

    private static final ForgeConfigSpec.IntValue ABBREVIATE_ABOVE = BUILDER
            .comment("Display module: with COMPACT, counts from this value up are abbreviated.",
                    "中文：显示模块：COMPACT 模式下，达到该数量即缩写。")
            .translation("ultrastacks.configuration.abbreviateAbove")
            .defineInRange("abbreviateAbove", 1000, 256, 1000);

    private static final ForgeConfigSpec.BooleanValue TOOLTIP_EXACT = BUILDER
            .comment("Display module: add the exact count and the stack limit to the item tooltip.",
                    "中文：显示模块：在物品提示框里显示准确数量与堆叠上限。")
            .translation("ultrastacks.configuration.tooltipExact")
            .define("tooltipExact", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile int maxStackSize = DEFAULT_MAX_STACK_SIZE;
    private static volatile int potionStackLimit = 1;
    private static volatile int enchantedBookStackLimit = 1;
    private static volatile int bucketStackLimit = 16;
    private static volatile int brewingYield = 3;
    private static volatile boolean scaleSmallItems = true;
    private static volatile boolean craftingModule = true;
    private static volatile boolean displayModule = true;
    private static volatile boolean bulkCrafting = true;
    private static volatile CountStyle countStyle = CountStyle.COMPACT;
    private static volatile int abbreviateAbove = 1000;
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
        potionStackLimit = resolveSpecialStackLimit(POTION_STACK_LIMIT.get(), requested);
        enchantedBookStackLimit = resolveSpecialStackLimit(ENCHANTED_BOOK_STACK_LIMIT.get(), requested);
        brewingYield = Math.max(1, BREWING_YIELD.get());
        if (requested > SAFE_MAX_STACK_SIZE && !EXPERIMENTAL.get()) {
            UltraStacks.LOGGER.warn("maxStackSize {} is above the safe value {}; using the safe value. "
                    + "Enable experimentalStackSizes to allow it.", requested, SAFE_MAX_STACK_SIZE);
            requested = SAFE_MAX_STACK_SIZE;
        }

        if (requested != maxStackSize) {
            UltraStacks.LOGGER.info("maxStackSize: {} -> {}", maxStackSize, requested);
            maxStackSize = requested;
        }
        potionStackLimit = resolveSpecialStackLimit(POTION_STACK_LIMIT.get(), requested);
        enchantedBookStackLimit = resolveSpecialStackLimit(ENCHANTED_BOOK_STACK_LIMIT.get(), requested);
        brewingYield = Math.max(1, BREWING_YIELD.get());
        int bucketRaw = BUCKET_STACK_LIMIT.get();
        bucketStackLimit = bucketRaw <= 0 ? scaledMaxForItem(16) : Math.min(bucketRaw, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);
    }

    /** Vanilla containers default to 99; walk that placeholder up to the configured limit. */
    public static int raiseVanillaDefault(int original) {
        return original == 99 ? maxStackSize : original;
    }

    /** Mods' hardcoded legacy limit (64/99); never lower, only raise. */
    public static int raiseLegacyDefault(int original) {
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /** New stack limit for an item whose vanilla limit is {@code vanillaMax}. */
    public static int scaledMaxForItem(int vanillaMax) {
        return StackLimits.resolve(vanillaMax, maxStackSize, scaleSmallItems);
    }

    /** Configured potion stack limit; 1 keeps vanilla behaviour. */
    public static int potionStackLimit() {
        return potionStackLimit;
    }

    /** Configured stack limit for other normally-unstackable items; 1 keeps vanilla behaviour. */
    public static int enchantedBookStackLimit() {
        return enchantedBookStackLimit;
    }

    /** Stack limit for filled buckets. */
    public static int bucketStackLimit() {
        return bucketStackLimit;
    }

    private static boolean isPotion(ItemStack stack) {
        try {
            return stack.getItem() instanceof net.minecraft.world.item.PotionItem;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isDamageable(ItemStack stack) {
        try {
            return stack.isDamageableItem();
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    /**
     * Final stack limit for a stack, given the vanilla limit it reported.
     *
     * <p>Items that already stack go through the usual scaling. Items that do not stack natively keep their
     * vanilla limit unless potionStackLimit / enchantedBookStackLimit raise them; damageable items (tools, armour)
     * always stay at one.
     *
     * <p><b>中文</b> 可堆叠物品走原有缩放；原生不可堆叠的物品只有在 potionStackLimit / enchantedBookStackLimit
     * 调高时才放大，工具与护甲永远保持 1。
     */
    public static int limitFor(ItemStack stack, int vanillaMax) {
        int scaled = scaledMaxForItem(vanillaMax);
        if (scaled > 1) {
            return scaled;
        }
        if (isDamageable(stack)) {
            return vanillaMax;
        }
        int configured;
        if (isPotion(stack)) {
            configured = potionStackLimit;
        } else if (isEnchantedBook(stack)) {
            configured = enchantedBookStackLimit;
        } else if (stack.getItem() instanceof net.minecraft.world.item.BucketItem
                || stack.getItem() instanceof net.minecraft.world.item.MilkBucketItem) {
            configured = Math.max(bucketStackLimit, scaledMaxForItem(16));
        } else {
            return vanillaMax;
        }
        return Math.max(vanillaMax, configured);
    }

    /** Bottles a single reagent converts into when brewing stacked potions. */
    public static int brewingYield() {
        return brewingYield;
    }

    /** 0 means automatic: one sixteenth of the configured stack limit. */
    private static int resolveSpecialStackLimit(int configured, int maxStackSize) {
        if (configured <= 0) {
            return Math.max(1, maxStackSize / 16);
        }
        return Math.max(1, Math.min(configured, UltraStacks.ABSOLUTE_MAX_STACK_SIZE));
    }

    /** True for enchanted books, the only non-potion single item that is raised on purpose. */
    public static boolean isEnchantedBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK);
    }
}
