package com.zaixiayesheng.ultrastacks.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.crafting.UnlockState;
import com.zaixiayesheng.ultrastacks.net.Network;
import com.zaixiayesheng.ultrastacks.net.UnlockMessage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「解锁整批合成」这个键。
     *
 * <p>The unlock key for finishing a whole batch. It defaults to mouse button 4 and can be rebound in
 * the vanilla controls screen. It never triggers an action by itself: it only reports its state on
 * press and release, and the server decides how much a shift-click may craft.
 */
@Mod.EventBusSubscriber(modid = UltraStacks.MOD_ID, value = Dist.CLIENT)
public final class UnlockKey {

    public static final KeyMapping UNLOCK = new KeyMapping(
            "key.ultrastacks.unlock",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.MOUSE,
            3,
            "key.categories.ultrastacks");

    private static boolean lastSent;

    private UnlockKey() {
    }

    @Mod.EventBusSubscriber(modid = UltraStacks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(UNLOCK);
        }
    }

    /** 每 tick 看一眼，变了才发包。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            lastSent = false;
            return;
        }
        boolean down = UNLOCK.isDown();
        if (down != lastSent) {
            lastSent = down;
            Network.CHANNEL.sendToServer(new UnlockMessage(down));
        }
    }

    /** 退出时清掉，免得名单越积越多。 */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UnlockState.clear(event.getEntity().getUUID());
    }
}
