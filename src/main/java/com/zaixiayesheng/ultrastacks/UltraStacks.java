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
 * 模组入口：把物品的堆叠上限抬到配置值（默认 65536），并让容器、槽位、存档与网络包接受这个数量。
 *
 * <p>构造器只做三件事：注册 COMMON 配置、挂上配置加载回调、挂上改写物品默认组件的监听器。
 *
 * <p>Mod entry point: it raises item stack limits to the configured value (65536 by default) and
 * makes containers, slots, saved data and network payloads accept that number.
 *
 * <p>The constructor does exactly three things: register the COMMON config, subscribe the
 * config-load callback and subscribe the listener that rewrites item default components.
 */
@Mod(UltraStacks.MOD_ID)
public final class UltraStacks {
    public static final String MOD_ID = "ultrastacks";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 存档与网络能承载的硬上限，与配置无关。
     *
     * <p>取 {@link Integer#MAX_VALUE} 的一半（1073741823）：两个上限相加仍在 int 范围内，
     * 菜单与合并逻辑里常见的 {@code a + b} 不会溢出。
     *
     * <p>{@code ItemStack} 的编解码器在类初始化时就已建好，早于配置文件加载，所以这里只能是编译期常量。
     *
     * <p>Hard ceiling that saved data and network payloads can carry, independent of the config.
     *
     * <p>It is half of {@link Integer#MAX_VALUE} (1073741823), so adding two limits still fits an
     * int and the {@code a + b} arithmetic common in menu and merge code cannot overflow.
     *
     * <p>{@code ItemStack}'s codecs are built during class initialisation, before the config file is
     * read, so this has to be a compile-time constant.
     */
    public static final int ABSOLUTE_MAX_STACK_SIZE = Integer.MAX_VALUE / 2;

    public UltraStacks(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, UltraStacksConfig.SPEC);
        modEventBus.addListener(UltraStacksConfig::onConfigLoad);
        modEventBus.addListener(ModBusEvents::onModifyDefaultComponents);
        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.registerConfigScreen(modContainer);
        }
    }
}
