package com.zaixiayesheng.ultrastacks.client;

import com.zaixiayesheng.ultrastacks.UltraStacksConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * 客户端专属的配置界面注册。
 *
 * <p>Forge 47 没有自带的 TOML 配置界面，这里只在检测到 Cloth Config 时注册自定义界面；没装就整个跳过，
 * 模组本身的功能不受影响。
 *
 * <p>Client-only config screen registration.
 *
 * <p>Forge 47 ships no TOML config screen, so this registers a custom one only when Cloth Config is
 * installed. Without it the registration is skipped entirely and the mod itself is unaffected.
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
