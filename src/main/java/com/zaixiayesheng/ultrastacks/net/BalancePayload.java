package com.zaixiayesheng.ultrastacks.net;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.crafting.GridBalance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 「帮我把合成格摊平」——只在服务端没装合成辅助时才有人理。
 *
 * <p>装了合成辅助的服务器有自己的 {@code BalanceMessage}，我们不去抢；没装的时候它那条包会被丢掉，
 * 客户端摊好的结果在下次同步时被冲回去，看着就像点不动。这个包补的就是那个缺口。
 *
 * <p>Empty request asking the server to flatten the crafting grid. Servers that ship
 * CraftingTweaks already have their own packet, so we stay out of the way there.
 */
public record BalancePayload() implements CustomPacketPayload {

    public static final Type<BalancePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UltraStacks.MOD_ID, "balance"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, BalancePayload> STREAM_CODEC =
            StreamCodec.unit(new BalancePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 服务端：有合成辅助就交给它，没有才自己算。 */
    public static void handle(BalancePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (ModList.get().isLoaded("craftingtweaks")) {
                // 它自己的 BalanceMessage 会把活干了，别再算一遍。
                return;
            }
            AbstractContainerMenu menu = player.containerMenu;
            GridBalance.Result result = GridBalance.balanceMenu(menu);
            if (result == null) {
                return;
            }
            menu.broadcastChanges();
            String line = String.format("balance server: %d slots, %d items", result.slots(), result.items());
            UltraStacks.LOGGER.info("[diag] {}", line);
            com.zaixiayesheng.ultrastacks.DiagLog.line("[diag] " + line);
        });
    }
}
