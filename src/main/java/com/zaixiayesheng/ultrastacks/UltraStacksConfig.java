package com.zaixiayesheng.ultrastacks;

import com.zaixiayesheng.ultrastacks.core.StackLimits;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 配置和换算都在这里：上限、特殊物品、各模块与兼容开关，加上 mixin 直接调的那些纯函数。
 * Config hub plus the pure helpers the mixins call — change a number here, not in each mixin.
 */
public final class UltraStacksConfig {
    public static final int DEFAULT_MAX_STACK_SIZE = 65536;
    public static final int SAFE_MAX_STACK_SIZE = 65536;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAX_STACK_SIZE = BUILDER
            .comment("Core - stack limit for stackable items (vanilla 64-size items).",
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

    private static final ModConfigSpec.IntValue POTION_STACK_LIMIT = BUILDER
            .comment("药水每堆最多几个（药水、喷溅、滞留、水瓶）。",
            "0 = Auto（自动）：堆叠上限的 1/16，默认 65536 时为 4096。",
            "1 = 原版（不可堆叠）；其它正数按填写的值生效。",
            "Potions only (potion, splash, lingering, water bottle). 0 = Auto, 1 = vanilla.")
        .translation("ultrastacks.configuration.potionStackLimit")
            .defineInRange("potionStackLimit", 0, 0, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    private static final ModConfigSpec.IntValue ENCHANTED_BOOK_STACK_LIMIT = BUILDER
            .comment("附魔书每堆最多几个。",
            "0 = Auto（自动）：堆叠上限的 1/16，默认 65536 时为 4096。",
            "1 = 原版（不可堆叠）；其它正数按填写的值生效。",
            "Enchanted books only. 0 = Auto, 1 = vanilla.")
        .translation("ultrastacks.configuration.enchantedBookStackLimit")
            .defineInRange("enchantedBookStackLimit", 0, 0, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);
    private static final ModConfigSpec.IntValue BUCKET_STACK_LIMIT = BUILDER
            .comment("各种桶的堆叠上限；低于空桶上限时自动抬到空桶。",
                    "Bucket stack limit; values below the empty bucket limit are raised to it.")
            .translation("ultrastacks.configuration.bucketStackLimit")
            .defineInRange("bucketStackLimit", 16, 1, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    private static final ModConfigSpec.IntValue BREWING_YIELD = BUILDER
            .comment("每个原料能酿出几瓶（原版为 3）。",
                    "Bottles one reagent converts into when brewing stacked potions; vanilla is 3.")
            .translation("ultrastacks.configuration.brewingYield")
            .defineInRange("brewingYield", 3, 1, 4096);

    private static final ModConfigSpec.BooleanValue CRAFTING_MODULE = BUILDER
            .comment("Optional module - crafting: one shift-click finishes a whole batch.",
                    "中文：可选模块 · 合成：一次 Shift 点击把整批合完。")
            .translation("ultrastacks.configuration.crafting")
            .define("crafting", true);

    private static final ModConfigSpec.BooleanValue DISPLAY_MODULE = BUILDER
            .comment("Optional module - display: stack counts drawn in the GUI.",
                    "中文：可选模块 · 显示：界面上的堆叠数量。")
            .translation("ultrastacks.configuration.display")
            .define("display", true);

    private static final ModConfigSpec.BooleanValue BULK_CRAFTING = BUILDER
            .comment("Optional module - crafting: bulk crafting. Off = vanilla one craft per click.",
                    "中文：可选模块 · 合成：批量模式；关闭后恢复原版一次一件。")
            .translation("ultrastacks.configuration.bulkCrafting")
            .define("bulkCrafting", true);

    static final ModConfigSpec.IntValue BULK_CRAFT_LIMIT = BUILDER
            .comment("Crafting: how many crafts one shift-click may do before the unlock key is held (default 64).",
                    "中文：合成：按住解锁键之前，一次 Shift 点击最多合成多少次（默认 64）。")
            .translation("ultrastacks.configuration.bulkCraftLimit")
            .defineInRange("bulkCraftLimit", 64, 1, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    static final ModConfigSpec.BooleanValue BULK_CRAFT_UNLOCK_KEY = BUILDER
            .comment("Crafting: require holding the unlock key (default mouse button 4) to craft the whole batch.",
                    "中文：合成：必须按住解锁键（默认鼠标侧键 4）才能一次合完整批。")
            .translation("ultrastacks.configuration.bulkCraftUnlockKey")
            .define("bulkCraftUnlockKey", true);

    public enum CountStyle {
        COMPACT,
        EXACT
    }

    private static final ModConfigSpec.EnumValue<CountStyle> COUNT_STYLE = BUILDER
            .comment("Optional module - display: COMPACT abbreviates large counts (12k), EXACT prints every digit.",
                    "中文：可选模块 · 显示：COMPACT 缩写（12k），EXACT 完整数字。")
            .translation("ultrastacks.configuration.countStyle")
            .defineEnum("countStyle", CountStyle.COMPACT);

    private static final ModConfigSpec.IntValue ABBREVIATE_ABOVE = BUILDER
            .comment("Optional module - display: with COMPACT, counts from this value up are abbreviated.",
                    "中文：可选模块 · 显示：COMPACT 模式下，达到该数量即缩写。")
            .translation("ultrastacks.configuration.abbreviateAbove")
            .defineInRange("abbreviateAbove", 1000, 256, 1000);

    private static final ModConfigSpec.BooleanValue TOOLTIP_EXACT = BUILDER
            .comment("Optional module - display: add the exact count and the stack limit to the item tooltip.",
                    "中文：可选模块 · 显示：在物品提示框里显示准确数量与堆叠上限。")
            .translation("ultrastacks.configuration.tooltipExact")
            .define("tooltipExact", true);

    private static final ModConfigSpec.BooleanValue COMPAT_SOPHISTICATED = BUILDER
            .comment("Compat patch - raise the slot limit of Sophisticated Storage and Backpacks to the configured stack size.",
                    "中文：兼容补丁 - 把精妙储存与背包的槽位上抬到配置值。")
            .translation("ultrastacks.configuration.compatSophisticated")
            .define("compat.sophisticated", true);

    private static final ModConfigSpec.BooleanValue COMPAT_AE2 = BUILDER
            .comment("Raise the slot limit of AE2 terminals, interfaces and item handlers; interface throughput follows maxStackSize / 16.",
                    "中文：把应用能源终端、接口与物品处理器的槽位上限抬到配置值；接口每槽容量取配置上限的十六分之一。")
            .translation("ultrastacks.configuration.compatAe2")
            .define("compat.ae2", true);

    private static final ModConfigSpec.BooleanValue COMPAT_REFINED_STORAGE = BUILDER
            .comment("Raise the slot limit of Refined Storage grids and item handlers to the configured stack size.",
                    "中文：把精致存储终端与物品处理器的槽位上限抬到配置值。")
            .translation("ultrastacks.configuration.compatRefinedStorage")
            .define("compat.refinedstorage", true);

    private static final ModConfigSpec.BooleanValue COMPAT_IRON_FURNACES = BUILDER
            .comment("Raise the input and output caps of Iron Furnaces so the furnaces take and make full stacks.",
                    "中文：把更多熔炉的输入、输出上限抬到配置值，熔炉才能吃下整摞。")
            .translation("ultrastacks.configuration.compatIronFurnaces")
            .define("compat.ironfurnaces", true);

    private static final ModConfigSpec.BooleanValue COMPAT_MYSTICAL_AGRICULTURE = BUILDER
            .comment("Raise the slot limit of Cucumber-based machines (Mystical Agriculture and its addons) to the configured stack size.",
                    "中文：把神秘农业及其附属机器的槽位上限抬到配置值。")
            .translation("ultrastacks.configuration.compatMysticalAgriculture")
            .define("compat.mysticalagriculture", true);



    private static final ModConfigSpec.IntValue CONFIG_VERSION = BUILDER
            .comment("Internal: configuration format version. Do not edit.",
                    "中文：内部使用：配置格式版本，请勿修改。")
            .translation("ultrastacks.configuration.configVersion")
            .defineInRange("configVersion", 0, 0, 100);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile int maxStackSize = DEFAULT_MAX_STACK_SIZE;
    private static volatile int potionStackLimit = 1;
    private static volatile int enchantedBookStackLimit = 1;
    private static volatile int bucketStackLimit = 16;
    private static volatile int brewingYield = 3;
    private static volatile boolean scaleSmallItems = true;
    private static volatile boolean craftingModule = true;
    private static volatile boolean displayModule = true;
    private static volatile boolean bulkCrafting = true;
    private static volatile int bulkCraftLimit = 64;
    private static volatile boolean bulkCraftUnlockKey = true;
    private static volatile CountStyle countStyle = CountStyle.COMPACT;
    private static volatile int abbreviateAbove = 1000;
    private static volatile boolean tooltipExact = true;
    private static volatile boolean compatSophisticated = true;
    private static volatile boolean compatAe2 = true;
    private static volatile boolean compatRefinedStorage = true;
    private static volatile boolean compatIronFurnaces = true;
    private static volatile boolean compatMysticalAgriculture = true;

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

    /** 一次 Shift 点击最多合成多少次（未按解锁键时）。 */
    public static int bulkCraftLimit() {
        return bulkCraftLimit;
    }

    /** 是否要求按住解锁键才能一次合完整批。 */
    public static boolean bulkCraftUnlockKey() {
        return bulkCraftUnlockKey;
    }

    public static CountStyle countStyle() {
        return countStyle;
    }

    public static int abbreviateAbove() {
        return abbreviateAbove;
    }


    /**
     * 精妙储存与精妙背包的槽位上限是否跟着配置走。
     *
     * <p>Whether the slot limits of Sophisticated Storage and Sophisticated Backpacks follow the
     * configured stack size.
     */
    public static boolean compatSophisticated() {
        return compatSophisticated;
    }

    /**
     * 把仍停在 64 或 99 的槽位上限抬到配置的堆叠上限，其它数值原样返回。
     *
     * <p>Raises a slot limit that is still on 64 or 99 to the configured stack size, and returns any
     * other value unchanged.
     */
    public static int raiseSophisticatedLimit(int original) {
        if (!compatSophisticated) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 应用能源的终端、接口与物品处理器是否跟着配置的堆叠上限走。
     *
     * <p>Whether the terminals, interfaces and item handlers of Applied Energistics 2 follow the
     * configured stack size.
     */
    public static boolean compatAe2() {
        return compatAe2;
    }

    /**
     * 精致存储的终端与物品处理器是否跟着配置的堆叠上限走。
     *
     * <p>Whether the grids and item handlers of Refined Storage follow the configured stack size.
     */
    public static boolean compatRefinedStorage() {
        return compatRefinedStorage;
    }

    /**
     * 把 64 或 99 这类原版上限换成配置值，其余数值原样返回。
     *
     * <p>Replaces a vanilla-sized limit of 64 or 99 with the configured value and returns any other
     * value unchanged.
     */
    public static int ae2SlotLimit(int original) {
        if (!compatAe2) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 单个槽位一次能搬多少：取配置上限的十六分之一，且不低于传进来的原值。
     *
     * <p>How much a single slot moves per operation: a sixteenth of the configured stack limit, never
     * below the value passed in.
     */
    public static long ae2TransferAmount(long original) {
        if (!compatAe2 || (original != 64L && original != 99L)) {
            return original;
        }
        return Math.max(original, Math.max(1L, maxStackSize / 16L));
    }

    /**
     * 接口每槽的容量：低于「配置上限 / 16」的抬到那个值，高于它的保持原样，但都不超过堆叠上限。
     *
     * <p>Per-slot interface capacity: anything below stack limit / 16 is lifted to it, larger values are
     * kept, and nothing exceeds the configured stack limit.
     */
    public static long ae2InterfaceCapacity(long original) {
        if (!compatAe2) {
            return original;
        }
        long raised = Math.max(original, Math.max(1L, maxStackSize / 16L));
        return Math.min(raised, Math.max(1L, maxStackSize));
    }

    /**
     * 把 64 或 99 这类原版上限换成配置值，其余数值原样返回。
     *
     * <p>Replaces a vanilla-sized limit of 64 or 99 with the configured value and returns any other
     * value unchanged.
     */
    public static int refinedStorageSlotLimit(int original) {
        if (!compatRefinedStorage) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 与 {@link #refinedStorageSlotLimit(int)} 同一套规则，只因为容量字段是 {@code long} 才单独提供。
     *
     * <p>The same rule as {@link #refinedStorageSlotLimit(int)}, provided separately only because the
     * capacity field is a {@code long}.
     */
    public static long refinedStorageCapacity(long original) {
        if (!compatRefinedStorage) {
            return original;
        }
        return original == 64L || original == 99L ? maxStackSize : original;
    }

    /**
     * 更多熔炉的输入、输出上限是否跟着配置的堆叠上限走。
     *
     * <p>Whether the input and output caps of Iron Furnaces follow the configured stack size instead of
     * staying at 64.
     */
    public static boolean compatIronFurnaces() {
        return compatIronFurnaces;
    }

    /**
     * 把 64 或 99 这类原版上限换成配置值，其余数值原样返回。
     *
     * <p>Replaces a vanilla-sized limit of 64 or 99 with the configured value and returns any other
     * value unchanged.
     */
    public static int ironFurnaceLimit(int original) {
        if (!compatIronFurnaces) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }


    /**
     * 把 64 或 99 这类原版上限换成配置值，其余数值原样返回。
     *
     * <p>Replaces a vanilla-sized limit of 64 or 99 with the configured value and returns any other
     * value unchanged.
     */
    public static int mysticalSlotLimit(int original) {
        if (!compatMysticalAgriculture) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
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
        bulkCraftLimit = BULK_CRAFT_LIMIT.get();
        bulkCraftUnlockKey = BULK_CRAFT_UNLOCK_KEY.get();
        countStyle = COUNT_STYLE.get();
        abbreviateAbove = ABBREVIATE_ABOVE.get();
        tooltipExact = TOOLTIP_EXACT.get();
        compatSophisticated = COMPAT_SOPHISTICATED.get();
        compatAe2 = COMPAT_AE2.get();
        compatRefinedStorage = COMPAT_REFINED_STORAGE.get();
        compatIronFurnaces = COMPAT_IRON_FURNACES.get();
        compatMysticalAgriculture = COMPAT_MYSTICAL_AGRICULTURE.get();
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
        bucketStackLimit = bucketRaw <= 0 ? scaledMax(16) : Math.min(bucketRaw, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);
        if (CONFIG_VERSION.get() < 2) {
            boolean migrated = false;
            if (POTION_STACK_LIMIT.get() == 1 && ENCHANTED_BOOK_STACK_LIMIT.get() == 1) {
                POTION_STACK_LIMIT.set(0);
                ENCHANTED_BOOK_STACK_LIMIT.set(0);
                potionStackLimit = resolveSpecialStackLimit(0, requested);
                enchantedBookStackLimit = resolveSpecialStackLimit(0, requested);
                migrated = true;
            }
            CONFIG_VERSION.set(2);
            SPEC.save();
            if (migrated) {
                UltraStacks.LOGGER.info("[UltraStacks] migrated the old stack-limit config: potions and enchanted books now use Auto");
            }
        }

    }

    public static int raiseVanillaDefault(int original) {
        return original == 99 ? maxStackSize : original;
    }

    /**
     * 槽位报出 64 或 99 时，把它当成「原版默认大小」换成配置的堆叠上限。
     *
     * <p>When a slot reports 64 or 99 it is treated as "the vanilla default size" and replaced with the
     * configured stack limit.
     */
    public static int raiseSlotLimit(int original) {
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 模组槽位处理器（{@code ItemStackHandler} 这一类）报出的上限：只升不降。
     *
     * <p>A limit reported by a modded slot handler such as {@code ItemStackHandler}: raise only, never
     * lower.
     */
    public static int raiseModdedSlotLimit(int original) {
        return Math.max(original, maxStackSize);
    }

    public static int codecUpperBound() {
        return UltraStacks.ABSOLUTE_MAX_STACK_SIZE;
    }

    public static int raiseCodecUpperBound(int bound) {
        return bound == 99 ? codecUpperBound() : bound;
    }

    /**
     * 去掉「等于原版默认值」的 {@code max_stack_size} 组件。
     *
     * <p>Drops a vanilla-default {@code max_stack_size} component so the stack matches recipes again.
     */
    public static ItemStack stripDefaultStackSize(ItemStack stack) {
        if (stack.isEmpty() || stack.isDamageableItem()) {
            return stack;
        }
        Integer stored = stack.get(DataComponents.MAX_STACK_SIZE);
        if (stored != null && stored >= 2 && stored <= 64) {
            stack.remove(DataComponents.MAX_STACK_SIZE);
        }
        return stack;
    }

    public static int scaledMax(int vanillaMax) {
        return StackLimits.resolve(vanillaMax, maxStackSize, scaleSmallItems);
    }

    /**
     * 给定堆叠在原版下报出的上限，算出它最终该有的上限。
     *
     * <p>Works out the final limit of a stack from the limit it reports in vanilla.
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

    public static int potionStackLimit() {
        return potionStackLimit;
    }

    public static int enchantedBookStackLimit() {
        return enchantedBookStackLimit;
    }

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
     * 一个堆叠最终的上限：先看常规缩放，再看几个需要特殊处理的原生不可堆叠物品。
     *
     * <p>The final limit of a stack: the ordinary scaling first, then the few natively unstackable items
     * that need special handling.
     */
    public static int limitFor(ItemStack stack, int vanillaMax) {
        int scaled = scaledMax(vanillaMax);
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
        } else if (stack.getItem() instanceof net.minecraft.world.item.BucketItem) {
            configured = Math.max(bucketStackLimit, scaledMax(16));
        } else {
            return vanillaMax;
        }
        return Math.max(vanillaMax, configured);
    }

    public static int brewingYield() {
        return brewingYield;
    }

    /**
     * 把「0 表示自动」的配置值换算成实际上限。
     *
     * <p>Turns a "0 means automatic" config value into the actual limit.
     */
    private static int resolveSpecialStackLimit(int configured, int maxStackSize) {
        if (configured <= 0) {
            return Math.max(1, maxStackSize / 16);
        }
        return Math.max(1, Math.min(configured, UltraStacks.ABSOLUTE_MAX_STACK_SIZE));
    }

    public static boolean isEnchantedBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK);
    }
}
