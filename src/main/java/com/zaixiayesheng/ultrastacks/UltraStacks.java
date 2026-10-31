package com.zaixiayesheng.ultrastacks;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * UltraStacks / 超大堆叠。
     *
 * <p>Raises the item stack limit to a configurable value (65536 by default) and makes containers,
 * slots, saved data and network packets actually accept it. The config spec is registered from this
 * class's constructor, and the numbers and conversions live in {@link UltraStacksConfig}.
 */
@Mod(UltraStacks.MOD_ID)
public final class UltraStacks {
    public static final String MOD_ID = "ultrastacks";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 存档与网络能承载的硬上限，与配置无关。
     *
     * <p>Hard ceiling for saved data and network packets, independent of the configuration: half of
     * {@link Integer#MAX_VALUE}, so an int addition such as {@code a + b} over two limits still cannot
     * overflow.
     */
    public static final int ABSOLUTE_MAX_STACK_SIZE = Integer.MAX_VALUE / 2;

    public UltraStacks() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, UltraStacksConfig.SPEC);
    }
}
