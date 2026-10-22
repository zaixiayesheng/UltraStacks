package com.zaixiayesheng.ultrastacks;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * UltraStacks / 超级堆叠
 *
 * <p>Raises the item stack limit to a configurable value and makes containers, slots and saved
 * data actually accept it.
 *
 * <p>This is the 1.0.0 core only: stacking itself plus the vanilla-side correctness fixes that
 * large stacks require.
 *
 * <p><b>EN</b> Mod entry point: registers the config and applies the configured stack limit to item defaults at load.
 */
@Mod(UltraStacks.MOD_ID)
public final class UltraStacks {
    public static final String MOD_ID = "ultrastacks";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Upper bound the save format and the network can carry, independent of the config.
     *
     * <p>{@code ItemStack.CODEC} is built during class initialisation, before the config file is
     * read, so the codec bound has to be a compile-time constant. Half of {@link Integer#MAX_VALUE}
     * keeps {@code a + b} style int arithmetic from overflowing; StackSizeTweaks and BiggerStacks
     * use the same value.
     */
    public static final int ABSOLUTE_MAX_STACK_SIZE = Integer.MAX_VALUE / 2;

    public UltraStacks(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, UltraStacksConfig.SPEC);
        modEventBus.addListener(UltraStacksConfig::onConfigLoad);
        modEventBus.addListener(ModBusEvents::onModifyDefaultComponents);
    }
}
