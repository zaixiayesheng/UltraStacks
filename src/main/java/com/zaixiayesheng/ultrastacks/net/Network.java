package com.zaixiayesheng.ultrastacks.net;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 目前就一个包：解锁键状态。版本号先写死 "1"，改结构再动。
 *
 * One payload for now — the unlock-key state. Version pinned to "1".
 */
public final class Network {

    private Network() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(UnlockPayload.TYPE, UnlockPayload.STREAM_CODEC, UnlockPayload::handle);
    }
}
