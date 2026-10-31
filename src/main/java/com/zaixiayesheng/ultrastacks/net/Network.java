package com.zaixiayesheng.ultrastacks.net;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 两个包：解锁键状态，和“帮我摊平合成格”。版本号先写死 "1"，改结构再动。
 *
 * Two payloads — the unlock key state and a “balance my grid” request. Version pinned to "1".
 */
public final class Network {

    private Network() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(UnlockPayload.TYPE, UnlockPayload.STREAM_CODEC, UnlockPayload::handle);
        registrar.playToServer(BalancePayload.TYPE, BalancePayload.STREAM_CODEC, BalancePayload::handle);
    }
}
