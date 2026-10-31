package com.zaixiayesheng.ultrastacks;

import com.mojang.logging.LogUtils;
import com.zaixiayesheng.ultrastacks.client.ClientSetup;
import com.zaixiayesheng.ultrastacks.client.UnlockKey;
import com.zaixiayesheng.ultrastacks.core.TickWatchdog;
import com.zaixiayesheng.ultrastacks.crafting.UnlockState;
import com.zaixiayesheng.ultrastacks.net.Network;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

/**
 * 模组入口：把物品的堆叠上限抬到配置值（默认 65536），并让容器、槽位、存档与网络包接受这个数量。
     *
 * <p>Mod entry point: it raises item stack limits to the configured value (65536 by default) and
 * makes containers, slots, saved data and network payloads accept that number.
 */
@Mod(UltraStacks.MOD_ID)
public final class UltraStacks {
    public static final String MOD_ID = "ultrastacks";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 存档与网络能承载的硬上限，与配置无关。
     *
     * <p>Hard ceiling that saved data and network payloads can carry, independent of the config.
     */
    public static final int ABSOLUTE_MAX_STACK_SIZE = Integer.MAX_VALUE / 2;

    public UltraStacks(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, UltraStacksConfig.SPEC);
        modEventBus.addListener(UltraStacksConfig::onConfigLoad);
        modEventBus.addListener(ModBusEvents::onModifyDefaultComponents);
        modEventBus.addListener(Network::register);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> TickWatchdog.serverTick());
        TickWatchdog.start();
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(UltraStacks::onPlayerLoggedOut);
        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.registerConfigScreen(modContainer);
            modEventBus.addListener(UnlockKey::register);
            NeoForge.EVENT_BUS.addListener(UnlockKey::onClientTick);
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event) -> TickWatchdog.clientTick());
        }
    }

    /** 玩家退出就把他从「按住解锁键」名单里划掉。 */
    private static void onPlayerLoggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        UnlockState.clear(event.getEntity().getUUID());
    }

    /** 注册 {@code /ultrastacks dump}：现场抓取物品栏完整 NBT 的诊断命令。 */
    private void onRegisterCommands(RegisterCommandsEvent event) {
        UltraStacksDumpCommand.register(event.getDispatcher());
    }
}
