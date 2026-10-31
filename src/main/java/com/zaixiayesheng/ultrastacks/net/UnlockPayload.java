package com.zaixiayesheng.ultrastacks.net;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.crafting.UnlockState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 客户端报一声“按住了解锁键 / 松开了”。状态没变就不发，平时没流量。
 *
 * Client tells the server whether the unlock key is held. Silent while the state stays the same.
 */
public record UnlockPayload(boolean unlocked) implements CustomPacketPayload {

    public static final Type<UnlockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UltraStacks.MOD_ID, "unlock"));

    public static final StreamCodec<ByteBuf, UnlockPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, UnlockPayload::unlocked, UnlockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 服务端收下，记一笔就完事。 */
    public static void handle(UnlockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null) {
                UnlockState.set(context.player().getUUID(), payload.unlocked());
            }
        });
    }
}
