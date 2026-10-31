package com.zaixiayesheng.ultrastacks.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端专属的配置界面注册。
     *
 * <p>Client-only config screen registration.
 */
public final class ClientSetup {
    private ClientSetup() {
    }

    /**
     * 把自带配置界面挂到模组容器上，模组列表里的「配置」按钮会用这个界面。
     *
     * <p>Hooks the built-in config screen onto the mod container; the mod list's config button opens it.
     */
    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
