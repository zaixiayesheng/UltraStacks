package com.zaixiayesheng.ultrastacks;

import com.mojang.logging.LogUtils;
import com.zaixiayesheng.ultrastacks.client.ClientSetup;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * 把物品堆叠上限抬到可配置的值，默认 65536。
 *
 * <p>只改那个数字是不够的：容器与槽位要对这个数量放行，存档与网络包要能承载它，合成、附魔、酿造这些
 * 按堆叠数推导代价的逻辑也要跟着走。本类只负责注册配置规格、挂上配置加载监听，具体改动分散在 core、
 * crafting、display 与 mixin 各包里。
 *
 * <p>Raises the item stack limit to a configurable value, 65536 by default.
 *
 * <p>Changing that number alone is not enough: containers and slots have to let that many items
 * through, saved data and network packets have to carry it, and the logic that derives cost from a
 * stack's count — crafting, enchanting, brewing — has to follow along. This class only registers the
 * config spec and hooks up the config-load listener; the actual changes live in the core, crafting,
 * display and mixin packages.
 */
@Mod(UltraStacks.MOD_ID)
public final class UltraStacks {
    public static final String MOD_ID = "ultrastacks";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 存档与网络能承载的硬上限，与配置无关。
     *
     * <p>{@code ItemStack.CODEC} 在类初始化时就建好了，早于配置文件加载，所以这个界不能来自配置，
     * 只能是编译期常量；取 {@link Integer#MAX_VALUE} 的一半，两个堆叠相加时 int 加法才不会溢出。
     *
     * <p>The hard bound that saved data and network packets can carry; it does not depend on config.
     *
     * <p>{@code ItemStack.CODEC} is built during class initialisation, before the config file is
     * loaded, so this bound cannot come from config — it has to be a compile-time constant. Half of
     * {@link Integer#MAX_VALUE} leaves room for two stacks to be added as ints without overflowing.
     */
    public static final int ABSOLUTE_MAX_STACK_SIZE = Integer.MAX_VALUE / 2;

    public UltraStacks(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, UltraStacksConfig.SPEC);
        modEventBus.addListener(UltraStacksConfig::onConfigLoad);
        if (FMLEnvironment.getDist().isClient()) {
            ClientSetup.registerConfigScreen(modContainer);
        }
    }
}
