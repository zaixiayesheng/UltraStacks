package com.zaixiayesheng.ultrastacks.net;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 1.20.1 的简单网络通道。
     *
 * <p>The simple channel for 1.20.1. It carries a single message — the unlock-key state. The protocol
 * version is pinned to "1"; bump it when the layout changes and mismatched clients get turned away.
 */
@Mod.EventBusSubscriber(modid = UltraStacks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Network {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UltraStacks.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private Network() {
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CHANNEL.messageBuilder(UnlockMessage.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnlockMessage::encode)
                .decoder(UnlockMessage::decode)
                .consumerMainThread(UnlockMessage::handle)
                .add());
    }
}
