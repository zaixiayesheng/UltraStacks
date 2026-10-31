package com.zaixiayesheng.ultrastacks.net;

import com.zaixiayesheng.ultrastacks.crafting.UnlockState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端告诉服务端「解锁键现在按住了 / 松开了」。
     *
 * <p>Tells the server whether the unlock key is held. 1.20.1 predates the {@code CustomPacketPayload}
 * API, so this is a plain record with a hand-written codec.
 */
public record UnlockMessage(boolean unlocked) {

    public static void encode(UnlockMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.unlocked());
    }

    public static UnlockMessage decode(FriendlyByteBuf buffer) {
        return new UnlockMessage(buffer.readBoolean());
    }

    /** 服务端收下，记一笔就完事。 */
    public static void handle(UnlockMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getSender() != null) {
            UnlockState.set(context.getSender().getUUID(), message.unlocked());
        }
        context.setPacketHandled(true);
    }
}
