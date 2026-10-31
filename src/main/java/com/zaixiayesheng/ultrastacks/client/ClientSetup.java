package com.zaixiayesheng.ultrastacks.client;

import com.zaixiayesheng.ultrastacks.UltraStacksConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * 客户端专属的配置界面注册。
     *
 * <p>Client-only config screen registration.
 */
public final class ClientSetup {
    private static final String CLOTH_CONFIG = "cloth_config";

    private ClientSetup() {
    }

    /**
     * 有 Cloth Config 时注册配置界面，否则什么也不做。
     *
     * <p>Registers the config screen when Cloth Config is installed; otherwise does nothing.
     */
    public static void registerConfigScreen() {
        if (!ModList.get().isLoaded(CLOTH_CONFIG)) {
            return;
        }
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(UltraStacksConfigScreen::create));
    }
}
