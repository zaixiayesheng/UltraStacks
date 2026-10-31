package com.zaixiayesheng.ultrastacks;

import com.mojang.logging.LogUtils;
import com.zaixiayesheng.ultrastacks.client.ClientSetup;
import com.zaixiayesheng.ultrastacks.client.UnlockKey;
import com.zaixiayesheng.ultrastacks.crafting.UnlockState;
import com.zaixiayesheng.ultrastacks.net.Network;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/**
 * 把物品堆叠上限抬到可配置的值，默认 65536。
     *
 * <p>Raises the item stack limit to a configurable value, 65536 by default.
 */
@Mod(UltraStacks.MOD_ID)
public final class UltraStacks {
    public static final String MOD_ID = "ultrastacks";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 存档与网络能承载的硬上限，与配置无关。
     *
     * <p>The hard bound that saved data and network packets can carry; it does not depend on config.
     */
    public static final int ABSOLUTE_MAX_STACK_SIZE = Integer.MAX_VALUE / 2;

    public UltraStacks(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, UltraStacksConfig.SPEC);
        modEventBus.addListener(UltraStacksConfig::onConfigLoad);
        modEventBus.addListener(Network::register);
        NeoForge.EVENT_BUS.addListener(UltraStacks::onPlayerLoggedOut);
        if (FMLEnvironment.getDist().isClient()) {
            ClientSetup.registerConfigScreen(modContainer);
            modEventBus.addListener(UnlockKey::register);
            NeoForge.EVENT_BUS.addListener(UnlockKey::onClientTick);
        }
    }

    /**
     * 玩家退出就把他的解锁状态划掉，免得 UUID 名单越攒越长。
     *
     * <p>Drops a player's unlock state on logout so the UUID set does not keep growing.
     */
    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UnlockState.clear(event.getEntity().getUUID());
    }
}
