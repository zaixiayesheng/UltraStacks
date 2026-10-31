package com.zaixiayesheng.ultrastacks.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.net.UnlockPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 解锁整批合成的按键，默认鼠标侧键 4，设置里能改。
 * 它自己不干别的，就把“按没按住”报给服务端。
     *
 * The bulk-craft unlock key (mouse 4 by default, rebindable).
 * All it does is tell the server whether it is held.
 */
public final class UnlockKey {

    /**
     * 用 UNIVERSAL：IN_GAME 开着界面就不认了，可按它的时候必然是开着合成界面的。
     *
     * UNIVERSAL, not IN_GAME — the latter is inactive while a screen is open, which is exactly
     * when this key is held.
     */
    public static final KeyMapping UNLOCK = new KeyMapping(
            "key.ultrastacks.unlock",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.MOUSE,
            3,
            "key.categories.ultrastacks");

    private static boolean lastSent;

    private UnlockKey() {
    }

    /** 注册按键，客户端总线调用。 */
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(UNLOCK);
    }

    /**
     * 每 tick 看一眼，变了才发包。
     * 鼠标键不能用 isDown()：开着界面时原版根本不更新它，永远停在松开。
     *
     * Polls once per tick and only sends when the state flips.
     * isDown() is useless for mouse buttons — vanilla stops updating them while a screen is open.
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            lastSent = false;
            return;
        }
        boolean down = isHeld();
        if (down != lastSent) {
            lastSent = down;
            DiagLog.line("[diag] unlock key held=" + down);
            PacketDistributor.sendToServer(new UnlockPayload(down));
        }
    }

    /** 这个键现在按着没有。 */
    private static boolean isHeld() {
        InputConstants.Key key = UNLOCK.getKey();
        if (key.getType() == InputConstants.Type.MOUSE) {
            long window = Minecraft.getInstance().getWindow().getWindow();
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return UNLOCK.isDown();
    }
}
