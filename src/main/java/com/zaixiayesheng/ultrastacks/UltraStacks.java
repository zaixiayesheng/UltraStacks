package com.zaixiayesheng.ultrastacks;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * UltraStacks / 超级堆叠
 *
 * <p>Raises the item stack limit to a configurable value and makes containers, slots, saved data
 * and network packets actually accept it.
 *
 * <p>1.0.0 core only: stacking itself plus the vanilla-side correctness fixes large stacks need.
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
     * <p>Half of {@link Integer#MAX_VALUE} keeps {@code a + b} style int arithmetic from
     * overflowing; StackSizeTweaks and BiggerStacks use the same value.
     */
    public static final int ABSOLUTE_MAX_STACK_SIZE = Integer.MAX_VALUE / 2;

    public UltraStacks() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, UltraStacksConfig.SPEC);
    }
}
