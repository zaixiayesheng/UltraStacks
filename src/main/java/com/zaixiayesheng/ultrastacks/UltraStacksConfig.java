package com.zaixiayesheng.ultrastacks;

import com.zaixiayesheng.ultrastacks.core.StackLimits;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 配置和换算都在这里：上限、特殊物品、各模块与兼容开关，加上 mixin 直接调的那些纯函数。
 * Config hub plus the pure helpers the mixins call — change a number here, not in each mixin.
 */
public final class UltraStacksConfig {
    /**
     * 默认堆叠上限，也是 {@code maxStackSize} 的默认值。
     *
     * <p>Default stack limit, also the default of {@code maxStackSize}.
     */
    public static final int DEFAULT_MAX_STACK_SIZE = 65536;
    /**
     * 没打开实验性选项时允许的最大堆叠上限，超过它的配置会被退回这个值。
     *
     * <p>Largest stack limit allowed while the experimental switch is off; a higher setting falls
     * back to this value.
     */
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

    private static final ModConfigSpec.IntValue BULK_CRAFT_LIMIT = BUILDER
            .comment("Crafting: how many crafts one shift-click may do before the unlock key is held (default 64).",
                    "中文：合成：按住解锁键之前，一次 Shift 点击最多合成多少次（默认 64）。")
            .translation("ultrastacks.configuration.bulkCraftLimit")
            .defineInRange("bulkCraftLimit", 64, 1, UltraStacks.ABSOLUTE_MAX_STACK_SIZE);

    private static final ModConfigSpec.BooleanValue BULK_CRAFT_UNLOCK_KEY = BUILDER
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

    private static final ModConfigSpec.BooleanValue COMPAT_MEKANICAL_CREATE = BUILDER
            .comment("Let Mekanical Create's fan output simulation follow the configured stack size.",
                    "中文：让通用动力的鼓风机产物模拟跟随配置的堆叠上限。")
            .translation("ultrastacks.configuration.compatMekanicalCreate")
            .define("compat.mekanicalcreate", true);

    private static final ModConfigSpec.BooleanValue COMPAT_MEKANISM = BUILDER
            .comment("Raise the input and output slot limit of Mekanism machines to the configured stack size.",
                    "中文：把通用机械机器的输入、输出槽位上限抬到配置值。")
            .translation("ultrastacks.configuration.compatMekanism")
            .define("compat.mekanism", true);

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

    private static final ModConfigSpec.BooleanValue COMPAT_IMMERSIVE_ENGINEERING = BUILDER
            .comment("Raise the slot limit of Immersive Engineering machines to the configured stack size.",
                    "中文：把沉浸工程机器的槽位上限抬到配置值。")
            .translation("ultrastacks.configuration.compatImmersiveEngineering")
            .define("compat.immersiveengineering", true);



    private static final ModConfigSpec.IntValue CONFIG_VERSION = BUILDER
            .comment("Internal: configuration format version. Do not edit.",
                    "中文：内部使用：配置格式版本，请勿修改。")
            .translation("ultrastacks.configuration.configVersion")
            .defineInRange("configVersion", 0, 0, 100);

    /**
     * 构建好的配置规格。主类构造器把它注册成 COMMON 配置，{@link #onConfigLoad} 也只认这一个规格。
     *
     * <p>The built config spec. The mod constructor registers it as the COMMON config, and
     * {@link #onConfigLoad} ignores every other spec.
     */
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


    public static boolean compatSophisticated() {
        return compatSophisticated;
    }

    public static boolean compatMekanicalCreate() {
        return compatMekanicalCreate;
    }

    /**
     * 把仍是原版 64 / 99 的槽位上限换成配置值，其它数值原样返回。
     *
     * <p>Replaces a slot limit that is still the vanilla 64 or 99 with the configured value and
     * returns every other number untouched.
     */
    public static int raiseSophisticatedLimit(int original) {
        if (!compatSophisticated) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    public static boolean compatMekanism() {
        return compatMekanism;
    }

    /**
     * 把仍是原版 64 / 99 的槽位上限换成配置值，其它数值原样返回。
     *
     * <p>Replaces a slot limit that is still the vanilla 64 or 99 with the configured value and
     * returns every other number untouched.
     */
    public static int raiseMekanismLimit(int original) {
        if (!compatMekanism) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    public static boolean compatAe2() {
        return compatAe2;
    }

    public static boolean compatRefinedStorage() {
        return compatRefinedStorage;
    }

    /**
     * 应用能源把终端与接口的槽位上限写死成 99，这里换成配置值。
     *
     * <p>AE2 hardcodes 99 for terminal and interface slot limits, so that number is replaced with
     * the configured value.
     */
    public static int ae2SlotLimit(int original) {
        if (!compatAe2) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 接口每槽一次能搬多少：取原值与应用能源默认值的较大者，默认值是配置上限的十六分之一。
     *
     * <p>How much one interface slot moves at a time: the larger of the original allowance and a
     * sixteenth of the configured stack size.
     */
    public static long ae2TransferAmount(long original) {
        if (!compatAe2 || (original != 64L && original != 99L)) {
            return original;
        }
        return Math.max(original, Math.max(1L, maxStackSize / 16L));
    }

    /**
     * ME 接口每槽能搬多少：低于「上限 / 16」的抬到那个值，高于它的保持原样，但不超过堆叠上限。
     *
     * <p>Per-slot ME interface capacity: anything below stack size / 16 is lifted to it, larger
     * values are kept, and nothing exceeds the configured stack size.
     */
    public static long ae2InterfaceCapacity(long original) {
        if (!compatAe2) {
            return original;
        }
        long raised = Math.max(original, Math.max(1L, maxStackSize / 16L));
        return Math.min(raised, Math.max(1L, maxStackSize));
    }

    /**
     * 精致存储自家的物品处理器也把上限报成 64 或 99，这里换成配置值。
     *
     * <p>Refined Storage's own item handlers report 64 or 99 as well, so those numbers are replaced
     * with the configured value. Every other number comes back unchanged, as it does while the
     * compat switch is off.
     */
    public static int refinedStorageSlotLimit(int original) {
        if (!compatRefinedStorage) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 精致存储的每格容量以 {@code long} 表示，判定条件与 {@link #refinedStorageSlotLimit} 相同。
     *
     * <p>Refined Storage reports per-slot capacity as a {@code long}; the same 64 / 99 check applies.
     * The original value comes back unchanged while the compat switch is off.
     */
    public static long refinedStorageCapacity(long original) {
        if (!compatRefinedStorage) {
            return original;
        }
        return original == 64L || original == 99L ? maxStackSize : original;
    }

    public static boolean compatIronFurnaces() {
        return compatIronFurnaces;
    }

    /**
     * 更多熔炉把输入、输出的上限写死成 64；这里只替换这一类「64 就是原版上限」的用法。
     *
     * <p>Iron Furnaces hardcodes 64 as the input and output cap; only that vanilla default is
     * replaced with the configured limit. Every other number comes back unchanged, as it does while
     * the compat switch is off.
     */
    public static int ironFurnaceLimit(int original) {
        if (!compatIronFurnaces) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }


    /**
     * Cucumber 的槽位处理器把每格上限默认成 64，物品叠得再高，依赖它的机器输入输出格也只收 64 个。
     *
     * <p>The Cucumber slot handler defaults every machine slot to 64, so machines built on it would
     * take only 64 items no matter how high the stacks go. Only that default is replaced; slots with
     * an explicitly chosen limit keep it, and the original value comes back while the compat switch
     * is off.
     */
    public static int mysticalSlotLimit(int original) {
        if (!compatMysticalAgriculture) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 沉浸工程的机器、容器与物品处理器默认每格 64，这里把仍是 64 / 99 的上限抬到配置值。
     *
     * <p>Immersive Engineering machines, containers and item handlers default to 64 per slot; the
     * 64 / 99 values are raised to the configured limit here. Every other number comes back
     * unchanged, as it does while the compat switch is off.
     */
    public static int ieSlotLimit(int original) {
        if (!compatImmersiveEngineering) {
            return original;
        }
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 卓越前线的载具后备箱用自己一个默认 64 的字段当「能不能放进」的门槛，堆大了就被挡住。
     *
     * <p>Superb Warfare's vehicle trunk gates placement on its own field that defaults to 64, so
     * big stacks are rejected. Only 64 / 99 get raised; other values (such as data-driven ones)
     * stay as the mod set them.
     */
    public static int vehicleSlotLimit(int original) {
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    public static boolean tooltipExact() {
        return tooltipExact;
    }

    /**
     * 把配置文件里的值读进内存中的 volatile 字段。
     *
     * <p>Copies the values from the config file into the volatile fields.
     */
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
        compatMekanicalCreate = COMPAT_MEKANICAL_CREATE.get();
        compatMekanism = COMPAT_MEKANISM.get();
        compatAe2 = COMPAT_AE2.get();
        compatRefinedStorage = COMPAT_REFINED_STORAGE.get();
        compatIronFurnaces = COMPAT_IRON_FURNACES.get();
        compatMysticalAgriculture = COMPAT_MYSTICAL_AGRICULTURE.get();
        compatImmersiveEngineering = COMPAT_IMMERSIVE_ENGINEERING.get();
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

    /**
     * 把容器默认返回的 99 换成配置值，其它数字原样返回。
     *
     * <p>Replaces the 99 that containers return by default with the configured value and returns
     * every other number unchanged.
     */
    public static int raiseVanillaDefault(int original) {
        return original == 99 ? maxStackSize : original;
    }

    /**
     * 槽位上限：原版报 64 或 99 都表示「默认容器大小」，调用方真正想要的是抬上去的值。
     *
     * <p>Slot limits: vanilla reporting 64 or 99 means "the default container size", and the caller
     * really wants the raised value.
     */
    public static int raiseSlotLimit(int original) {
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * {@code ItemStackHandler} 这类自己保管上限的容器报的值：只升不降。
     *
     * <p>Values reported by containers that own their limit, such as {@code ItemStackHandler}: raise
     * only, never lower.
     */
    public static int raiseModdedSlotLimit(int original) {
        return Math.max(original, maxStackSize);
    }

    /**
     * 编解码器允许的最大数量，直接取硬上限 {@link UltraStacks#ABSOLUTE_MAX_STACK_SIZE}。
     *
     * <p>Largest count the codecs accept: the hard ceiling {@link UltraStacks#ABSOLUTE_MAX_STACK_SIZE}.
     */
    public static int codecUpperBound() {
        return UltraStacks.ABSOLUTE_MAX_STACK_SIZE;
    }

    /**
     * 只把原版编解码器写死的 99 换成硬上限，其余数值保持不动。
     *
     * <p>Replaces only the 99 hardcoded in the vanilla codec with the hard ceiling and keeps every
     * other value unchanged.
     */
    public static int raiseCodecUpperBound(int bound) {
        return bound == 99 ? codecUpperBound() : bound;
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

    public static boolean isPotion(ItemStack stack) {
        return stack.getItem() instanceof PotionItem;
    }

    /**
     * 默认组件应用之后，{@code ItemStack#getMaxStackSize()} 最终报给调用方的值。
     *
     * <p>The value {@code ItemStack#getMaxStackSize()} finally reports once the default components have
     * been applied.
     */
    public static int limitFor(ItemStack stack, int current) {
        if (stack.isDamageableItem()) {
            return current;
        }
        if (current > 1) {
            if (isVanillaDefault(current) && stack.getCount() > current) {
                return Math.max(current, scaledMax(current));
            }
            return current;
        }
        int configured;
        if (isPotion(stack)) {
            configured = potionStackLimit;
        } else if (isEnchantedBook(stack)) {
            configured = enchantedBookStackLimit;
        } else if (stack.getItem() instanceof net.minecraft.world.item.BucketItem
                || stack.getItem() instanceof net.minecraft.world.item.MilkBucketItem) {
            configured = Math.max(bucketStackLimit, scaledMax(16));
        } else {
            return current;
        }
        return Math.max(current, configured);
    }

    /**
     * 按原版上限算出配置下的上限，是 {@link StackLimits#resolve} 的薄封装。
     *
     * <p>Thin wrapper around {@link StackLimits#resolve}: turns a vanilla limit into the configured
     * one.
     */

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
        if (stored == null) {
            return stack;
        }
        // get() 读到的是合并视图（patch 叠在烘入默认上），干净栈也会返回 65536；
        // 只有 patch 里真的带这个组件才算「脏」，否则直接放行，避免刷屏。
        // 注意：get() 在缺组件时可能返回 null（而非 Optional.empty()），必须先判空。
        var patched = stack.getComponentsPatch().get(DataComponents.MAX_STACK_SIZE);
        if (patched == null || patched.isEmpty()) {
            return stack;
        }
        String patch = stack.getComponentsPatch().toString();
        int storedValue = stored;
        if (storedValue >= 2 && storedValue <= 64) {
            stack.remove(DataComponents.MAX_STACK_SIZE);
            com.zaixiayesheng.ultrastacks.UltraStacks.LOGGER.info(
                    "[diag] stripped max_stack_size={} from {} x{} (item default {}), patch before: {}",
                    storedValue, stack.getItem(), stack.getCount(), stack.getItem().getDefaultMaxStackSize(), patch);
        } else if (storedValue == stack.getItem().getDefaultMaxStackSize()) {
            // patch 值与烘入默认一致（如 65536）：多余但无害，静默剥掉归一化。
            stack.remove(DataComponents.MAX_STACK_SIZE);
        } else {
            com.zaixiayesheng.ultrastacks.UltraStacks.LOGGER.warn(
                    "[diag] unusual max_stack_size={} on {} x{} (item default {}), patch: {}",
                    storedValue, stack.getItem(), stack.getCount(), stack.getItem().getDefaultMaxStackSize(), patch);
        }
        return stack;
    }

    private static boolean isVanillaDefault(int value) {
        return value == 2 || value == 4 || value == 8 || value == 16 || value == 32 || value == 64;
    }

    /**
     * 切堆用的物品侧上限：与 {@link #limitFor} 同一套换算，但不看物品数量——数量为 1 的排序 key 栈
     * 也必须报出「这摞实际能堆多大」，否则整理会把大堆按原版默认值切碎。
     *
     * <p>The item-side split limit used by sorting: the same scaling as {@link #limitFor} but without the
     * count gate — a count-1 sorting key stack must still report how big a whole stack of this item can be,
     * otherwise sorting chops big stacks by the vanilla default.
     */
    public static int sortCapacity(ItemStack stack) {
        if (stack.isDamageableItem()) {
            // 有耐久的物品没有「一堆」这回事，永远按 1 算，别跟着栈里数量走。
            // Damageable items have no such thing as a stack: always 1, never following the count.
            return 1;
        }
        int base = stack.getItem().getDefaultMaxStackSize();
        Integer component = stack.get(DataComponents.MAX_STACK_SIZE);
        if (component != null) {
            base = component;
        }
        if (base <= 1) {
            // 药水 / 附魔书 / 桶走各自配置；其它上限 1 的物品保持 1。
            return limitFor(stack, base);
        }
        if (base == 2 || base == 4 || base == 8 || base == 16 || base == 32 || base == 64) {
            return Math.max(base, scaledMax(base));
        }
        return base;
    }

    public static int scaledMax(int vanillaMax) {
        return StackLimits.resolve(vanillaMax, maxStackSize, scaleSmallItems);
    }

    public static int brewingYield() {
        return brewingYield;
    }

    /**
     * 药水与附魔书的上限：0 表示自动，取堆叠上限的十六分之一；正数按原值，并夹在 1 与硬上限之间。
     *
     * <p>0 means automatic: one sixteenth of the stack limit; otherwise the configured value, clamped
     * between 1 and the hard ceiling.
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
