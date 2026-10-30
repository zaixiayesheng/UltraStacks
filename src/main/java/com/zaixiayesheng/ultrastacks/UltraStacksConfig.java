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
 * 配置入口：堆叠上限、特殊物品上限、各模块开关与兼容开关都在这里。
 *
 * <p>下面的 helper 只做纯换算，不碰世界状态，被 mixin 直接调用；换算规则集中在这一处，
 * 改数值时不必再翻各个 mixin。
 *
 * <p>Config hub: the stack limit, the limits for special items, the module switches and the
 * compat switches all live here.
 *
 * <p>The helpers below are pure conversions called straight from the mixins and never touch world
 * state. Keeping the rules in one place means a number can be changed without editing every
 * mixin.
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
     * <p>精妙储存与背包把每格上限写死成 64 或 99，换成配置值后才能真正装满一摞；
     * 兼容开关关闭时不做任何改动。
     *
     * <p>Replaces a slot limit that is still the vanilla 64 or 99 with the configured value and
     * returns every other number untouched.
     *
     * <p>Sophisticated Storage and Backpacks hardcode 64 or 99 per slot, so only the replacement
     * lets a slot actually fill a whole stack. Nothing changes while the compat switch is off.
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
     * <p>通用机械的机器槽位默认按 64 报上限，不换掉的话整摞物品进不了机器；
     * 兼容开关关闭时不做任何改动。
     *
     * <p>Replaces a slot limit that is still the vanilla 64 or 99 with the configured value and
     * returns every other number untouched.
     *
     * <p>Mekanism machine slots report 64 by default, and without the replacement a full stack
     * cannot enter a machine. Nothing changes while the compat switch is off.
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
     * <p>只认 64 与 99 这两个默认值，其它数字原样返回；兼容开关关闭时也不做任何改动。
     *
     * <p>AE2 hardcodes 99 for terminal and interface slot limits, so that number is replaced with
     * the configured value.
     *
     * <p>Only the two defaults, 64 and 99, are recognised; any other number comes back unchanged, as
     * does everything while the compat switch is off.
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
     * <p>只对 64 和 99 生效——那是应用能源自己的默认值；其它数字视为别人特意设定的结果，不动。
     * 兼容开关关闭时原样返回。
     *
     * <p>How much one interface slot moves at a time: the larger of the original allowance and a
     * sixteenth of the configured stack size.
     *
     * <p>Only the 64 and 99 defaults are touched; any other number is treated as a deliberate setting
     * and left alone. With the compat switch off the original value comes back unchanged.
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
     * <p>超大接口的容量可能比普通接口还低，这里给两者一个共同的下限；兼容开关关闭时原样返回。
     *
     * <p>Per-slot ME interface capacity: anything below stack size / 16 is lifted to it, larger
     * values are kept, and nothing exceeds the configured stack size.
     *
     * <p>An oversized interface can end up below a normal one, so both get the same floor. With the
     * compat switch off the original value comes back unchanged.
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
     * <p>其它数值原样返回，兼容开关关闭时也一样。
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
     * <p>其它数值原样返回，兼容开关关闭时也一样。
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
     * <p>只换掉这个默认值，显式设过上限的槽位保持原样；兼容开关关闭时原样返回。
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
     * <p>其它数值原样返回，兼容开关关闭时也一样。
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

    public static boolean tooltipExact() {
        return tooltipExact;
    }

    /**
     * 把配置文件里的值读进内存中的 volatile 字段。
     *
     * <p>只理会本模组的规格，别的模组加载配置时直接返回。上限超过 {@link #SAFE_MAX_STACK_SIZE}
     * 又没开实验性选项时退回安全值并警告；药水与附魔书配置 0 表示自动，按上限的十六分之一算。
     * 最后跑一次格式迁移：版本低于 2 且两项特殊上限都还停在原版 1 时改成自动并保存，
     * 免得旧配置里写死的 1 把药水永久限制在不可堆叠。
     *
     * <p>Copies the values from the config file into the volatile fields.
     *
     * <p>Only this mod's spec is handled; loading any other mod's config returns immediately. A limit
     * above {@link #SAFE_MAX_STACK_SIZE} falls back to the safe value with a warning unless the
     * experimental switch is on, and 0 for potions or enchanted books means automatic, i.e. one
     * sixteenth of the limit. The format migration runs last: a version below 2 whose two special
     * limits still sit at the vanilla 1 is moved to automatic and saved, so an old config cannot pin
     * potions to non-stacking forever.
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
     * <p>原版 {@code Container#getMaxStackSize()} 用 99 表示「默认容量」，所以只认这一个数字；
     * 真正按物品区分的单格上限由 {@link #raiseSlotLimit} 处理。
     *
     * <p>Replaces the 99 that containers return by default with the configured value and returns
     * every other number unchanged.
     *
     * <p>Vanilla's {@code Container#getMaxStackSize()} uses 99 to mean "default capacity", so only
     * that number is recognised; the per-item slot limit is handled by {@link #raiseSlotLimit}.
     */
    public static int raiseVanillaDefault(int original) {
        return original == 99 ? maxStackSize : original;
    }

    /**
     * 槽位上限：原版报 64 或 99 都表示「默认容器大小」，调用方真正想要的是抬上去的值。
     *
     * <p>本来就只放一件的槽位（药水、工具）返回 1，保持原样。
     *
     * <p>Slot limits: vanilla reporting 64 or 99 means "the default container size", and the caller
     * really wants the raised value.
     *
     * <p>Slots that legitimately hold one item (potions, tools) report 1 and stay untouched.
     */
    public static int raiseSlotLimit(int original) {
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * {@code ItemStackHandler} 这类自己保管上限的容器报的值：只升不降。
     *
     * <p>取配置值与原值的较大者，避免把作者特意设过的小上限抹掉。
     *
     * <p>Values reported by containers that own their limit, such as {@code ItemStackHandler}: raise
     * only, never lower.
     *
     * <p>The larger of the configured value and the original wins, so a deliberately small limit is
     * not wiped out.
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
     * <p>本来就能堆叠的物品（上限大于 1）和可损坏物品原样返回；只有原版上限为 1 的物品才可能被抬高，
     * 而且仅限配置里点名的那几类：药水、附魔书、桶。桶还要求不低于「原版 16 个一堆按比例换算」的结果，
     * 所以配置值偏小时不会把桶压小。
     *
     * <p>The value {@code ItemStack#getMaxStackSize()} finally reports once the default components have
     * been applied.
     *
     * <p>Items that already stack (a limit above 1) and damageable items are returned as they are.
     * Only items whose vanilla limit is 1 can be raised, and only for the kinds the config names:
     * potions, enchanted books and buckets. Buckets additionally never go below the scaled
     * equivalent of the vanilla 16, so a smaller config value cannot shrink them.
     */
    public static int limitFor(ItemStack stack, int current) {
        if (stack.isDamageableItem()) {
            return current;
        }
        if (current > 1) {
            return isVanillaDefault(current) ? Math.max(current, scaledMax(current)) : current;
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
     * <p>物品默认组件改写与桶上限换算都走这里，保证全模组只有一套换算规则。
     *
     * <p>Thin wrapper around {@link StackLimits#resolve}: turns a vanilla limit into the configured
     * one.
     *
     * <p>Both the item default component rewrite and the bucket limit calculation go through it, so
     * the whole mod shares one scaling rule.
     */
    private static boolean isVanillaDefault(int value) {
        return value == 2 || value == 4 || value == 8 || value == 16 || value == 32 || value == 64;
    }

    /**
     * 去掉「等于原版默认值」的 {@code max_stack_size} 组件。
     *
     * <p>旧存档里的抽屉模板会把这个组件固化成 64，带组件的物品和干净物品不是同一个栈，合成表与 JEI 都按组件
     * 比对，于是配方对不上。这里把它清掉，让物品回到普通形态。
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
