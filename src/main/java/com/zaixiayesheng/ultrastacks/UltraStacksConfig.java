package com.zaixiayesheng.ultrastacks;

import com.zaixiayesheng.ultrastacks.core.StackLimits;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 配置和换算都在这里：上限、特殊物品、各模块与兼容开关，加上 mixin 直接调的那些纯函数。
 * Config hub plus the pure helpers the mixins call — change a number here, not in each mixin.
 */
@Mod.EventBusSubscriber(modid = UltraStacks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class UltraStacksConfig {
    public static final int DEFAULT_MAX_STACK_SIZE = 65536;
    public static final int SAFE_MAX_STACK_SIZE = 65536;

    static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static final ForgeConfigSpec.IntValue MAX_STACK_SIZE = BUILDER
            .comment("Core - stack limit for stackable items (vanilla 64-size items).",
                    "Range: 64 - " + UltraStacks.ABSOLUTE_MAX_STACK_SIZE)
            .translation("ultrastacks.configuration.maxStackSize")
            .defineInRange("maxStackSize", DEFAULT_MAX_STACK_SIZE, 64, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    static final ForgeConfigSpec.BooleanValue SCALE_SMALL_ITEMS = BUILDER
            .comment("Scale smaller vanilla stacks proportionally (16-size items become a quarter of maxStackSize).")
            .translation("ultrastacks.configuration.scaleSmallItems")
            .define("scaleSmallItems", true);

    static final ForgeConfigSpec.BooleanValue EXPERIMENTAL = BUILDER
            .comment("Allow maxStackSize above " + SAFE_MAX_STACK_SIZE + ".",
                    "Experimental: mods that assume small stacks may misbehave or lose items.")
            .translation("ultrastacks.configuration.experimentalStackSizes")
            .define("experimentalStackSizes", false);

    static final ForgeConfigSpec.IntValue POTION_STACK_LIMIT = BUILDER
            .comment("药水每堆最多几个（药水、喷溅、滞留、水瓶）。",
            "0 = Auto（自动）：堆叠上限的 1/16，默认 65536 时为 4096。",
            "1 = 原版（不可堆叠）；其它正数按填写的值生效。",
            "Potions only (potion, splash, lingering, water bottle). 0 = Auto, 1 = vanilla.")
        .translation("ultrastacks.configuration.potionStackLimit")
            .defineInRange("potionStackLimit", 0, 0, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    static final ForgeConfigSpec.IntValue ENCHANTED_BOOK_STACK_LIMIT = BUILDER
            .comment("附魔书每堆最多几个。",
            "0 = Auto（自动）：堆叠上限的 1/16，默认 65536 时为 4096。",
            "1 = 原版（不可堆叠）；其它正数按填写的值生效。",
            "Enchanted books only. 0 = Auto, 1 = vanilla.")
        .translation("ultrastacks.configuration.enchantedBookStackLimit")
            .defineInRange("enchantedBookStackLimit", 0, 0, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);
    static final ForgeConfigSpec.IntValue BUCKET_STACK_LIMIT = BUILDER
            .comment("各种桶的堆叠上限；低于空桶上限时自动抬到空桶。",
                    "Bucket stack limit; values below the empty bucket limit are raised to it.")
            .translation("ultrastacks.configuration.bucketStackLimit")
            .defineInRange("bucketStackLimit", 16, 1, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    static final ForgeConfigSpec.IntValue BREWING_YIELD = BUILDER
            .comment("每个原料能酿出几瓶（原版为 3）。",
                    "Bottles one reagent converts into when brewing stacked potions; vanilla is 3.")
            .translation("ultrastacks.configuration.brewingYield")
            .defineInRange("brewingYield", 3, 1, 4096);

    static final ForgeConfigSpec.BooleanValue CRAFTING_MODULE = BUILDER
            .comment("Optional module - crafting: one shift-click finishes a whole batch.",
                    "中文：可选模块 · 合成：一次 Shift 点击把整批合完。")
            .translation("ultrastacks.configuration.crafting")
            .define("crafting", true);

    static final ForgeConfigSpec.BooleanValue DISPLAY_MODULE = BUILDER
            .comment("Optional module - display: stack counts drawn in the GUI.",
                    "中文：可选模块 · 显示：界面上的堆叠数量。")
            .translation("ultrastacks.configuration.display")
            .define("display", true);

    static final ForgeConfigSpec.BooleanValue BULK_CRAFTING = BUILDER
            .comment("Optional module - crafting: bulk crafting. Off = vanilla one craft per click.",
                    "中文：可选模块 · 合成：批量模式；关闭后恢复原版一次一件。")
            .translation("ultrastacks.configuration.bulkCrafting")
            .define("bulkCrafting", true);

    static final ForgeConfigSpec.IntValue BULK_CRAFT_LIMIT = BUILDER
            .comment("Crafting: how many crafts one shift-click may do before the unlock key is held (default 64).",
                    "中文：合成：按住解锁键之前，一次 Shift 点击最多合成多少次（默认 64）。")
            .translation("ultrastacks.configuration.bulkCraftLimit")
            .defineInRange("bulkCraftLimit", 64, 1, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    static final ForgeConfigSpec.BooleanValue BULK_CRAFT_UNLOCK_KEY = BUILDER
            .comment("Crafting: require holding the unlock key (default mouse button 4) to craft the whole batch.",
                    "中文：合成：必须按住解锁键（默认鼠标侧键 4）才能一次合完整批。")
            .translation("ultrastacks.configuration.bulkCraftUnlockKey")
            .define("bulkCraftUnlockKey", true);

    public enum CountStyle {
        COMPACT,
        EXACT
    }

    static final ForgeConfigSpec.EnumValue<CountStyle> COUNT_STYLE = BUILDER
            .comment("Optional module - display: COMPACT abbreviates large counts (12k), EXACT prints every digit.",
                    "中文：可选模块 · 显示：COMPACT 缩写（12k），EXACT 完整数字。")
            .translation("ultrastacks.configuration.countStyle")
            .defineEnum("countStyle", CountStyle.COMPACT);

    static final ForgeConfigSpec.IntValue ABBREVIATE_ABOVE = BUILDER
            .comment("Optional module - display: with COMPACT, counts from this value up are abbreviated.",
                    "中文：可选模块 · 显示：COMPACT 模式下，达到该数量即缩写。")
            .translation("ultrastacks.configuration.abbreviateAbove")
            .defineInRange("abbreviateAbove", 1000, 256, 1000);

    static final ForgeConfigSpec.BooleanValue TOOLTIP_EXACT = BUILDER
            .comment("Optional module - display: add the exact count and the stack limit to the item tooltip.",
                    "中文：可选模块 · 显示：在物品提示框里显示准确数量与堆叠上限。")
            .translation("ultrastacks.configuration.tooltipExact")
            .define("tooltipExact", true);

    static final ForgeConfigSpec.BooleanValue COMPAT_SOPHISTICATED = BUILDER
            .comment("Compat patch - raise the slot limit of Sophisticated Storage and Backpacks to the configured stack size.",
                    "中文：兼容补丁 - 把精妙储存与背包的槽位上抬到配置值。")
            .translation("ultrastacks.configuration.compatSophisticated")
            .define("compat.sophisticated", true);

    static final ForgeConfigSpec.BooleanValue COMPAT_MEKANICAL_CREATE = BUILDER
            .comment("Let Mekanical Create's fan output simulation follow the configured stack size.",
                    "中文：让通用动力的鼓风机产物模拟跟随配置的堆叠上限。")
            .translation("ultrastacks.configuration.compatMekanicalCreate")
            .define("compat.mekanicalcreate", true);

    static final ForgeConfigSpec.BooleanValue COMPAT_MEKANISM = BUILDER
            .comment("Raise the input and output slot limit of Mekanism machines to the configured stack size.",
                    "中文：把通用机械机器的输入、输出槽位上限抬到配置值。")
            .translation("ultrastacks.configuration.compatMekanism")
            .define("compat.mekanism", true);

    static final ForgeConfigSpec.BooleanValue COMPAT_AE2 = BUILDER
            .comment("Raise the slot limit of AE2 terminals, interfaces and item handlers; interface throughput follows maxStackSize / 16.",
                    "中文：把应用能源终端、接口与物品处理器的槽位上限抬到配置值；接口每槽容量取配置上限的十六分之一。")
            .translation("ultrastacks.configuration.compatAe2")
            .define("compat.ae2", true);

    static final ForgeConfigSpec.BooleanValue COMPAT_REFINED_STORAGE = BUILDER
            .comment("Raise the slot limit of Refined Storage grids and item handlers to the configured stack size.",
                    "中文：把精致存储终端与物品处理器的槽位上限抬到配置值。")
            .translation("ultrastacks.configuration.compatRefinedStorage")
            .define("compat.refinedstorage", true);

    static final ForgeConfigSpec.BooleanValue COMPAT_IRON_FURNACES = BUILDER
            .comment("Raise the input and output caps of Iron Furnaces so the furnaces take and make full stacks.",
                    "中文：把更多熔炉的输入、输出上限抬到配置值，熔炉才能吃下整摞。")
            .translation("ultrastacks.configuration.compatIronFurnaces")
            .define("compat.ironfurnaces", true);

    static final ForgeConfigSpec.BooleanValue COMPAT_MYSTICAL_AGRICULTURE = BUILDER
            .comment("Raise the slot limit of Cucumber-based machines (Mystical Agriculture and its addons) to the configured stack size.",
                    "中文：把神秘农业及其附属机器的槽位上限抬到配置值。")
            .translation("ultrastacks.configuration.compatMysticalAgriculture")
            .define("compat.mysticalagriculture", true);

    static final ForgeConfigSpec.BooleanValue COMPAT_IMMERSIVE_ENGINEERING = BUILDER
            .comment("Raise the slot limit of Immersive Engineering machines to the configured stack size.",
                    "中文：把沉浸工程机器的槽位上限抬到配置值。")
            .translation("ultrastacks.configuration.compatImmersiveEngineering")
            .define("compat.immersiveengineering", true);



    static final ForgeConfigSpec.IntValue CONFIG_VERSION = BUILDER
            .comment("Internal: configuration format version. Do not edit.",
                    "中文：内部使用：配置格式版本，请勿修改。")
            .translation("ultrastacks.configuration.configVersion")
            .defineInRange("configVersion", 0, 0, 100);

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
    private static volatile int bulkCraftLimit = 64;
    private static volatile boolean bulkCraftUnlockKey = true;
    private static volatile CountStyle countStyle = CountStyle.COMPACT;
    private static volatile int abbreviateAbove = 1000;
    private static volatile boolean tooltipExact = true;
    private static volatile boolean compatSophisticated = true;
    private static volatile boolean compatMekanicalCreate = true;
    private static volatile boolean compatMekanism = true;
    private static volatile boolean compatAe2 = true;
    private static volatile boolean compatRefinedStorage = true;
    private static volatile boolean compatIronFurnaces = true;
    private static volatile boolean compatMysticalAgriculture = true;
    private static volatile boolean compatImmersiveEngineering = true;

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
     * 精妙储存与背包的槽位是否跟随配置的堆叠上限。
     *
     * <p>Whether the slots of Sophisticated Storage and Backpacks follow the configured stack size.
     */
    public static boolean compatSophisticated() {
        return compatSophisticated;
    }

    /**
     * 通用动力的鼓风机产物模拟是否跟随配置的堆叠上限。
     *
     * <p>Whether Mekanical Create's fan output simulation follows the configured stack size.
     */
    public static boolean compatMekanicalCreate() {
        return compatMekanicalCreate;
    }

    /**
     * 把报出 64 或 99 的槽位上限抬到配置的堆叠上限，其它数值原样返回。
     *
     * <p>Raises a slot limit that still reports 64 or 99 to the configured stack size and returns any
     * other value unchanged.
     */
    public static int raiseSophisticatedLimit(int original) {
        if (!compatSophisticated) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 通用机械机器的槽位上限是否跟随配置值。
     *
     * <p>Whether the slot limit of Mekanism machines follows the configured stack size.
     */
    public static boolean compatMekanism() {
        return compatMekanism;
    }

    /**
     * 把报出 64 或 99 的槽位上限抬到配置的堆叠上限，其它数值原样返回。
     *
     * <p>Raises a slot limit that still reports 64 or 99 to the configured stack size and returns any
     * other value unchanged.
     */
    public static int raiseMekanismLimit(int original) {
        if (!compatMekanism) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 应用能源的终端、接口与物品处理器是否跟随配置值。
     *
     * <p>Whether AE2 terminals, interfaces and item handlers follow the configured stack size.
     */
    public static boolean compatAe2() {
        return compatAe2;
    }

    /**
     * 精致存储的终端与物品处理器是否跟随配置值。
     *
     * <p>Whether Refined Storage grids and item handlers follow the configured stack size.
     */
    public static boolean compatRefinedStorage() {
        return compatRefinedStorage;
    }

    /**
     * 应用能源的终端与接口把槽位上限写死成 64 或 99，这里换成配置的堆叠上限。
     *
     * <p>AE2 reports 64 or 99 as the slot limit of its terminals and interfaces; those two values are
     * replaced with the configured stack size, everything else is kept.
     */
    public static int ae2SlotLimit(int original) {
        if (!compatAe2) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 接口每槽一次能搬多少：请求值为 64 或 99 时，取它与「配置上限的十六分之一」中的较大者。
     *
     * <p>How much one interface slot moves at a time: for a request of 64 or 99 it returns the larger of
     * the request and a sixteenth of the configured stack size; any other request passes through.
     */
    public static long ae2TransferAmount(long original) {
        if (!compatAe2 || (original != 64L && original != 99L)) {
            return original;
        }
        return Math.max(original, Math.max(1L, maxStackSize / 16L));
    }

    /**
     * 接口每槽的容量：先抬到「配置上限的十六分之一」这个下限，再夹在 1 与配置上限之间。
     *
     * <p>Per-slot interface capacity: lifted to a floor of a sixteenth of the configured stack size,
     * then clamped between one and the configured stack size, so a smaller value is raised and a larger
     * one is capped.
     */
    public static long ae2InterfaceCapacity(long original) {
        if (!compatAe2) {
            return original;
        }
        long raised = Math.max(original, Math.max(1L, maxStackSize / 16L));
        return Math.min(raised, Math.max(1L, maxStackSize));
    }

    /**
     * 精致存储的物品处理器同样按 64 或 99 报槽位上限，这里换成配置的堆叠上限。
     *
     * <p>Refined Storage's item handlers also report 64 or 99 as their slot limit; those two values are
     * replaced with the configured stack size.
     */
    public static int refinedStorageSlotLimit(int original) {
        if (!compatRefinedStorage) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 精致存储 3.x 的每格容量是 long，报出 64 或 99 时同样换成配置的堆叠上限。
     *
     * <p>Per-slot capacity on Refined Storage 3.x is a {@code long}; a reported 64 or 99 is replaced
     * with the configured stack size just like the {@code int} variants.
     */
    public static long refinedStorageCapacity(long original) {
        if (!compatRefinedStorage) {
            return original;
        }
        return original == 64L || original == 99L ? maxStackSize : original;
    }

    /**
     * 更多熔炉是否跟随配置值，而不是它自己写死的 64。
     *
     * <p>Whether Iron Furnaces follows the configured stack size instead of its own 64 cap.
     */
    public static boolean compatIronFurnaces() {
        return compatIronFurnaces;
    }

    /**
     * 更多熔炉把输入、输出的上限写死成 64，这里只替换 64 或 99 这种「就是原版上限」的取值。
     *
     * <p>Iron Furnaces hardcodes 64 as its input and output cap; only values that mean "the vanilla
     * limit", namely 64 and 99, are raised to the configured stack size.
     */
    public static int ironFurnaceLimit(int original) {
        if (!compatIronFurnaces) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }


    /**
     * Cucumber 的槽位处理器把每格上限默认成 64，物品叠得再高，机器的输入输出格也只收 64 个。
     * 这里只换掉这个默认值，显式设过上限的槽保持原样。
     *
     * <p>Cucumber's slot handler defaults every machine slot to 64, so a machine only takes 64 items
     * however high the stack is. Only that default, reported as 64 or 99, is replaced with the
     * configured stack size; a slot with an explicit limit keeps it.
     */
    public static int mysticalSlotLimit(int original) {
        if (!compatMysticalAgriculture) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 沉浸工程的机器、容器与物品处理器按 64 或 99 报槽位上限，这里换成配置的堆叠上限。
     *
     * <p>Immersive Engineering's machines, containers and item handlers report 64 or 99 per slot; those
     * values are replaced with the configured stack size.
     */
    public static int ieSlotLimit(int original) {
        if (!compatImmersiveEngineering) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    public static boolean tooltipExact() {
        return tooltipExact;
    }

    /**
     * 配置加载与重载时把 spec 里的值搬进静态字段。
     *
     * <p>Copies the spec values into the static fields on load and reload. The ordering matters:
     * {@code maxStackSize} has to settle first because the proportional limits (potions, enchanted
     * books, buckets) are derived from it, which is why they are computed twice - once before
     * {@code requested} may be clamped to the safe value and once with the final number. A request
     * above {@link #SAFE_MAX_STACK_SIZE} without the experimental switch is lowered to the safe value
     * with a warning.
     */
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        applyValues();
    }

    /**
     * 从规格里读出全部数值；配置加载与界面保存后都会调用。
     *
     * <p>Reads every value out of the spec; called on config load and again after the screen saves.
     */
    static void applyValues() {
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
        compatMekanicalCreate = COMPAT_MEKANICAL_CREATE.get();
        compatMekanism = COMPAT_MEKANISM.get();
        compatAe2 = COMPAT_AE2.get();
        compatRefinedStorage = COMPAT_REFINED_STORAGE.get();
        compatIronFurnaces = COMPAT_IRON_FURNACES.get();
        compatMysticalAgriculture = COMPAT_MYSTICAL_AGRICULTURE.get();
        compatImmersiveEngineering = COMPAT_IMMERSIVE_ENGINEERING.get();
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

    /**
     * 配置界面保存后写回磁盘，并立刻重新应用一遍数值。
     *
     * <p>Writes the spec back to disk and re-applies every value right away.
     */
    static void saveFromScreen() {
        SPEC.save();
        applyValues();
    }


    public static int raiseVanillaDefault(int original) {
        return original == 99 ? maxStackSize : original;
    }

    /**
     * 把模组写死的旧上限（64 或 99）抬到配置值，绝不调低。
     *
     * <p>Raises a mod's hardcoded legacy limit (64 or 99) to the configured value and never lowers it.
     */
    public static int raiseLegacyDefault(int original) {
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    public static int scaledMaxForItem(int vanillaMax) {
        return StackLimits.resolve(vanillaMax, maxStackSize, scaleSmallItems);
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
     * 一个堆叠最终生效的上限，入参是它自己报出的原版上限。
     *
     * <p>Final limit for a stack, given the vanilla limit it reported. Items that already stack go
     * through the usual scaling in {@link #scaledMaxForItem}. When that yields at most one, damageable
     * items (tools, armour) keep their vanilla limit, potions, enchanted books and buckets are raised to
     * {@code max(vanilla, configured)} with their own setting, and any other non-stackable item is
     * returned unchanged.
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

    public static int brewingYield() {
        return brewingYield;
    }

    /**
     * 药水、附魔书这类「单独配」的上限：0 表示自动，取配置上限的十六分之一（默认 65536 时为 4096）；
     * 其它值夹在 1 与硬上限之间。
     *
     * <p>The separately configured potion and enchanted-book limits: zero means automatic, a sixteenth of
     * the configured stack limit (4096 at the default 65536), and anything else is clamped between one
     * and the hard ceiling.
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
