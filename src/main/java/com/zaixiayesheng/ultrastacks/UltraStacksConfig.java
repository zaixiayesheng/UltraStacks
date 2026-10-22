package com.zaixiayesheng.ultrastacks;

import com.zaixiayesheng.ultrastacks.core.StackLimits;
import net.minecraftforge.common.ForgeConfigSpec;
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

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile int maxStackSize = DEFAULT_MAX_STACK_SIZE;
    private static volatile boolean scaleSmallItems = true;

    private UltraStacksConfig() {
    }

    public static int maxStackSize() {
        return maxStackSize;
    }

    public static boolean scaleSmallItems() {
        return scaleSmallItems;
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        int requested = Math.min(MAX_STACK_SIZE.get(), UltraStacks.ABSOLUTE_MAX_STACK_SIZE);
        scaleSmallItems = SCALE_SMALL_ITEMS.get();
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

    /** Mods' hardcoded legacy limit (64/99); never lower, only raise. */
    public static int raiseLegacyDefault(int original) {
        return original == 64 || original == 99 ? maxStackSize : original;
    }

    /** New stack limit for an item whose vanilla limit is {@code vanillaMax}. */
    public static int scaledMaxForItem(int vanillaMax) {
        return StackLimits.resolve(vanillaMax, maxStackSize, scaleSmallItems);
    }
}
