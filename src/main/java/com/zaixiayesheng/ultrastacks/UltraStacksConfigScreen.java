package com.zaixiayesheng.ultrastacks;

import java.util.function.Consumer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 1.20.1 的配置界面，用 Cloth Config 搭出来，作为可选联动：没装 Cloth Config 就不会实例化本类。
 *
 * <p>三个分类对应 README 的功能划分：核心、可选模块、兼容补丁。保存时调用
 * {@link UltraStacksConfig#saveFromScreen()}，写盘并立即重新读出数值。
 *
 * <p>The 1.20.1 config screen, built with Cloth Config as an optional integration; this class is
 * only instantiated when Cloth Config is installed.
 *
 * <p>The three categories mirror the README's split: core, optional modules and compat patches.
 * Saving calls {@link UltraStacksConfig#saveFromScreen()}, which writes the file and re-reads every
 * value immediately.
 */
public final class UltraStacksConfigScreen {
    private UltraStacksConfigScreen() {
    }

    /**
     * 构造界面；{@code parent} 是打开本界面的上一级屏幕，用来返回。
     *
     * <p>Builds the screen; {@code parent} is the screen it was opened from.
     */
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("ultrastacks.configuration.title"))
                .setSavingRunnable(UltraStacksConfig::saveFromScreen);
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory core = builder.getOrCreateCategory(
                Component.translatable("ultrastacks.configuration.category.core"));
        core.addEntry(intEntry(entries, "maxStackSize", UltraStacksConfig.MAX_STACK_SIZE.get(),
                65536, 64, UltraStacks.ABSOLUTE_MAX_STACK_SIZE, UltraStacksConfig.MAX_STACK_SIZE::set));
        core.addEntry(boolEntry(entries, "scaleSmallItems", UltraStacksConfig.SCALE_SMALL_ITEMS.get(),
                true, UltraStacksConfig.SCALE_SMALL_ITEMS::set));
        core.addEntry(boolEntry(entries, "experimentalStackSizes", UltraStacksConfig.EXPERIMENTAL.get(),
                false, UltraStacksConfig.EXPERIMENTAL::set));
        core.addEntry(intEntry(entries, "potionStackLimit", UltraStacksConfig.POTION_STACK_LIMIT.get(),
                0, 0, UltraStacks.ABSOLUTE_MAX_STACK_SIZE, UltraStacksConfig.POTION_STACK_LIMIT::set));
        core.addEntry(intEntry(entries, "enchantedBookStackLimit",
                UltraStacksConfig.ENCHANTED_BOOK_STACK_LIMIT.get(), 0, 0, UltraStacks.ABSOLUTE_MAX_STACK_SIZE,
                UltraStacksConfig.ENCHANTED_BOOK_STACK_LIMIT::set));
        core.addEntry(intEntry(entries, "bucketStackLimit", UltraStacksConfig.BUCKET_STACK_LIMIT.get(),
                16, 1, UltraStacks.ABSOLUTE_MAX_STACK_SIZE, UltraStacksConfig.BUCKET_STACK_LIMIT::set));
        core.addEntry(intEntry(entries, "brewingYield", UltraStacksConfig.BREWING_YIELD.get(),
                3, 1, 4096, UltraStacksConfig.BREWING_YIELD::set));

        ConfigCategory modules = builder.getOrCreateCategory(
                Component.translatable("ultrastacks.configuration.category.modules"));
        modules.addEntry(boolEntry(entries, "crafting", UltraStacksConfig.CRAFTING_MODULE.get(),
                true, UltraStacksConfig.CRAFTING_MODULE::set));
        modules.addEntry(boolEntry(entries, "display", UltraStacksConfig.DISPLAY_MODULE.get(),
                true, UltraStacksConfig.DISPLAY_MODULE::set));
        modules.addEntry(boolEntry(entries, "bulkCrafting", UltraStacksConfig.BULK_CRAFTING.get(),
                true, UltraStacksConfig.BULK_CRAFTING::set));
        modules.addEntry(entries.startEnumSelector(
                        Component.translatable("ultrastacks.configuration.countStyle"),
                        UltraStacksConfig.CountStyle.class, UltraStacksConfig.COUNT_STYLE.get())
                .setDefaultValue(UltraStacksConfig.CountStyle.COMPACT)
                .setSaveConsumer(UltraStacksConfig.COUNT_STYLE::set)
                .build());
        modules.addEntry(intEntry(entries, "abbreviateAbove", UltraStacksConfig.ABBREVIATE_ABOVE.get(),
                1000, 256, 1000, UltraStacksConfig.ABBREVIATE_ABOVE::set));
        modules.addEntry(boolEntry(entries, "tooltipExact", UltraStacksConfig.TOOLTIP_EXACT.get(),
                true, UltraStacksConfig.TOOLTIP_EXACT::set));

        ConfigCategory compat = builder.getOrCreateCategory(
                Component.translatable("ultrastacks.configuration.category.compat"));
        compat.addEntry(boolEntry(entries, "compatSophisticated", UltraStacksConfig.COMPAT_SOPHISTICATED.get(),
                true, UltraStacksConfig.COMPAT_SOPHISTICATED::set));
        compat.addEntry(boolEntry(entries, "compatMekanicalCreate",
                UltraStacksConfig.COMPAT_MEKANICAL_CREATE.get(), true,
                UltraStacksConfig.COMPAT_MEKANICAL_CREATE::set));
        compat.addEntry(boolEntry(entries, "compatMekanism", UltraStacksConfig.COMPAT_MEKANISM.get(),
                true, UltraStacksConfig.COMPAT_MEKANISM::set));
        compat.addEntry(boolEntry(entries, "compatAe2", UltraStacksConfig.COMPAT_AE2.get(),
                true, UltraStacksConfig.COMPAT_AE2::set));
        compat.addEntry(boolEntry(entries, "compatRefinedStorage",
                UltraStacksConfig.COMPAT_REFINED_STORAGE.get(), true,
                UltraStacksConfig.COMPAT_REFINED_STORAGE::set));
        compat.addEntry(boolEntry(entries, "compatIronFurnaces",
                UltraStacksConfig.COMPAT_IRON_FURNACES.get(), true,
                UltraStacksConfig.COMPAT_IRON_FURNACES::set));
        compat.addEntry(boolEntry(entries, "compatMysticalAgriculture",
                UltraStacksConfig.COMPAT_MYSTICAL_AGRICULTURE.get(), true,
                UltraStacksConfig.COMPAT_MYSTICAL_AGRICULTURE::set));
        compat.addEntry(boolEntry(entries, "compatImmersiveEngineering",
                UltraStacksConfig.COMPAT_IMMERSIVE_ENGINEERING.get(), true,
                UltraStacksConfig.COMPAT_IMMERSIVE_ENGINEERING::set));

        return builder.build();
    }

    private static IntegerListEntry intEntry(ConfigEntryBuilder entries, String key, int value,
                                             int fallback, int min, int max, Consumer<Integer> save) {
        return entries.startIntField(Component.translatable("ultrastacks.configuration." + key), value)
                .setDefaultValue(fallback)
                .setMin(min)
                .setMax(max)
                .setSaveConsumer(save)
                .build();
    }

    private static BooleanListEntry boolEntry(ConfigEntryBuilder entries, String key, boolean value,
                                              boolean fallback, Consumer<Boolean> save) {
        return entries.startBooleanToggle(Component.translatable("ultrastacks.configuration." + key), value)
                .setDefaultValue(fallback)
                .setSaveConsumer(save)
                .build();
    }
}
