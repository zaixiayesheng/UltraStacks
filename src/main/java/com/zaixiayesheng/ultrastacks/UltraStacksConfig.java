package com.zaixiayesheng.ultrastacks;

import com.zaixiayesheng.ultrastacks.core.StackLimits;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 配置入口：堆叠上限、特殊物品上限、各模块开关与兼容开关都在这里。
 *
 * <p>下面的 helper 只做纯换算、不碰世界状态，被 mixin 直接调用，所有换算规则集中在这一处，改起来只有
 * 一个地方要看。
 *
 * <p>Config hub: the stack limit, the special-item limits, the module switches and the compat switches
 * all live here.
 *
 * <p>The helpers below are pure conversions that never touch world state; the mixins call them directly.
 * Keeping every conversion rule in one place means there is only one spot to change.
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
     * <p>这里把 64 与 99 当作「还是原版默认值」来处理，其余数值一律视为已被显式设置而保留，免得覆盖玩家
     * 给单个槽位设的限制。兼容开关关闭时整体不生效。
     *
     * <p>Raises a slot limit that is still on 64 or 99 to the configured stack size, and returns any
     * other value unchanged.
     *
     * <p>64 and 99 are treated here as "still on the vanilla default", while every other value is taken
     * to be explicitly configured and is kept, so that a per-slot limit the player set is never
     * overridden. The whole conversion is skipped when the compat switch is off.
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
     * <p>应用能源的槽位上限取的就是这两种值之一，物品叠到 65536 之后它们会把每格卡回 64 或 99，所以这
     * 里跟着配置一起放大。
     *
     * <p>Replaces a vanilla-sized limit of 64 or 99 with the configured value and returns any other
     * value unchanged.
     *
     * <p>Applied Energistics 2 slots report one of those two values, which would otherwise clamp every
     * slot back to 64 or 99 once items stack to 65536, so they grow along with the config.
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
     * <p>只处理 64 与 99 这两种原版尺寸的上限，其它数值直接返回；这样「每 tick 搬一堆」在原版尺寸下
     * 不会变成一次搬几万个，堆叠调大以后也不会停在 64 上。兼容开关关闭时同样原样返回。
     *
     * <p>How much a single slot moves per operation: a sixteenth of the configured stack limit, never
     * below the value passed in.
     *
     * <p>Only limits of 64 and 99 — the two vanilla sizes — are handled, and anything else is returned
     * as is. This keeps "move one stack per operation" from turning into tens of thousands of items at
     * vanilla sizes, while still not getting stuck at 64 once the stack limit is raised. The value is
     * also returned unchanged when the compat switch is off.
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
     * <p>容量既要跟着堆叠上限一起变大，又不能比原版尺寸下更小：界面上的一个槽位在堆叠放大后仍要能用
     * 同一套逻辑搬货，所以设了个共同的下限；上限则卡在堆叠上限，避免单槽容量超过一摞物品。兼容开关关闭
     * 时原样返回。
     *
     * <p>Per-slot interface capacity: anything below stack limit / 16 is lifted to it, larger values are
     * kept, and nothing exceeds the configured stack limit.
     *
     * <p>The capacity has to grow together with the stack limit but must never end up smaller than it
     * was at vanilla sizes: a slot on the interface still moves items by the same rule after the limit
     * is raised, so a common floor is set, while the ceiling sits at the stack limit so that one slot
     * never holds more than one stack. The value is returned unchanged when the compat switch is off.
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
     * <p>精致存储的物品处理器同样按原版尺寸报上限，不换的话每格只收 64 或 99 个。兼容开关关闭时原样
     * 返回。
     *
     * <p>Replaces a vanilla-sized limit of 64 or 99 with the configured value and returns any other
     * value unchanged.
     *
     * <p>Refined Storage item handlers also report vanilla-sized limits, so without this replacement
     * each slot would take only 64 or 99 items. The value is returned unchanged when the compat switch
     * is off.
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
     * <p>int 版本套不到 long 字段上，所以这里复制一份同样的替换逻辑，两边的 64 / 99 判断必须保持一致。
     *
     * <p>The same rule as {@link #refinedStorageSlotLimit(int)}, provided separately only because the
     * capacity field is a {@code long}.
     *
     * <p>The int version does not fit a long field, so the replacement logic is duplicated here and the
     * 64 / 99 checks in both places have to stay in sync.
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
     * <p>只替换 64 与 99 这两个原版尺寸的数字，其它数值原样返回，这样同一个类里表示升级倍率之类的数字
     * 不会被顺手改掉。兼容开关关闭时原样返回。
     *
     * <p>Replaces a vanilla-sized limit of 64 or 99 with the configured value and returns any other
     * value unchanged.
     *
     * <p>Only the two vanilla-sized numbers 64 and 99 are replaced and everything else is passed
     * through, so a number that stands for something else in the same class — an upgrade multiplier, for
     * instance — is not changed along the way. The value is returned unchanged when the compat switch is
     * off.
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
     * <p>神秘农业的机器格子经 Cucumber 报上限时用的就是这两档原版尺寸，物品叠得再高，机器的输入输出格
     * 也只会收 64 个，所以这里跟着配置一起放大；显式设过上限的槽位不受影响。兼容开关关闭时原样返回。
     *
     * <p>Replaces a vanilla-sized limit of 64 or 99 with the configured value and returns any other
     * value unchanged.
     *
     * <p>The machine slots of Mystical Agriculture report their limit through Cucumber, which uses
     * those two vanilla sizes; however high items stack, a machine input or output slot would still take
     * only 64 of them, so the limit grows along with the config. Slots with an explicitly configured
     * limit are left alone, and the value is returned unchanged when the compat switch is off.
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
     * <p>真正只能放一个的槽位（药水、工具）报的是 1，返回值与之不同，因此不会被误抬到 65536。
     *
     * <p>When a slot reports 64 or 99 it is treated as "the vanilla default size" and replaced with the
     * configured stack limit.
     *
     * <p>Slots that genuinely hold a single item — potions, tools — report 1 and are therefore never
     * raised to 65536 by mistake.
     */
    public static int raiseSlotLimit(int original) {
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /**
     * 模组槽位处理器（{@code ItemStackHandler} 这一类）报出的上限：只升不降。
     *
     * <p>这类处理器本来就允许模组自己设一个比原版大的上限，用 {@code Math.max} 而不是「等于 64 才替换」，
     * 是为了不把别人已经调高的值压回去；代价是关掉再打开兼容开关时无法恢复原值，所以这里不提供降级。
     *
     * <p>A limit reported by a modded slot handler such as {@code ItemStackHandler}: raise only, never
     * lower.
     *
     * <p>Such handlers already let a mod pick a limit larger than vanilla, so {@code Math.max} is used
     * instead of the "replace only when it equals 64" rule, which would push an already raised value back
     * down. The trade-off is that toggling the compat switch off again cannot restore the original value,
     * which is why no downgrade path is offered here.
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

    public static int scaledMax(int vanillaMax) {
        return StackLimits.resolve(vanillaMax, maxStackSize, scaleSmallItems);
    }

    /**
     * 给定堆叠在原版下报出的上限，算出它最终该有的上限。
     *
     * <p>先按原版上限判断：不超过 1 的直接原样返回；可损毁物品（工具、护甲）也保持原样，否则它们会在
     * 同一格里叠起来，耐久条和修复逻辑都会出问题；其余走 {@link #scaledMax(int)}，并且取缩放结果与原版
     * 上限的较大者，永远不把物品改小。
     *
     * <p>注册物品时物品组件往往还没绑定，读取 {@code isDamageableItem()} 可能抛异常；此时退回原版上限而
     * 不是让整个注册过程崩掉。
     *
     * <p>Works out the final limit of a stack from the limit it reports in vanilla.
     *
     * <p>The vanilla limit is checked first: anything at 1 or below is returned as is, and so are
     * damageable items such as tools and armour — raising them would let them stack in one slot, which
     * breaks the durability bar and the repair logic. Everything else goes through
     * {@link #scaledMax(int)}, and the result is the larger of the scaled value and the vanilla limit, so
     * no item is ever made smaller.
     *
     * <p>Item components are often still unbound while the game registers items, so reading
     * {@code isDamageableItem()} may throw; in that case the vanilla limit is used instead of letting the
     * whole registration fail.
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
     * <p>缩放结果大于 1 说明这个物品本来就能堆，直接用它。剩下的都是原版放不进一格的物品：可损毁的保持
     * 原版（工具、护甲不能叠）；药水、附魔书、桶各取自己的配置上限，其中桶还要和 16 的缩放结果取较大者，
     * 免得装好液体的桶比空桶还能少装；其它不认识的物品原样返回。
     *
     * <p>最后一步与原版上限取较大者，配置填得比原版还小也不会把物品改小。
     *
     * <p>The final limit of a stack: the ordinary scaling first, then the few natively unstackable items
     * that need special handling.
     *
     * <p>A scaled result above 1 means the item already stacked, so it is used directly. What remains are
     * items vanilla never stacks: damageable ones keep their vanilla limit, since tools and armour must
     * not stack; potions, enchanted books and buckets each take their own configured limit, and for
     * buckets that value is taken against the scaled limit of a 16-size item so that a filled bucket can
     * never hold fewer than an empty one; anything unrecognised is returned as is.
     *
     * <p>The last step takes the larger of that value and the vanilla limit, so a config below the
     * vanilla limit still cannot shrink an item.
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
     * <p>0 取堆叠上限的十六分之一（至少 1），这样默认 65536 时得到 4096；正数按填写的值生效，并夹在 1
     * 与硬上限之间。负数按自动处理，因为配置项本身只允许填 0 及以上。
     *
     * <p>Turns a "0 means automatic" config value into the actual limit.
     *
     * <p>0 yields a sixteenth of the stack limit (at least 1), which gives 4096 at the default 65536; a
     * positive value is used as written and clamped between 1 and the hard bound. Negative values are
     * treated as automatic, although the config entry itself only accepts 0 and above.
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
