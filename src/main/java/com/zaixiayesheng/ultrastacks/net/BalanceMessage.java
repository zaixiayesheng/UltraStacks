package com.zaixiayesheng.ultrastacks.net;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.crafting.GridBalance;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

/**
 * 「帮我把合成格摊平」——只在服务端没装合成辅助时才有人理。
 *
 * <p>装了合成辅助的服务器有自己的 {@code BalanceMessage}，我们不去抢；没装的时候它那条包会被丢掉，
 * 客户端摊好的结果在下次同步时被冲回去，看着就像点不动。这个包补的就是那个缺口。
 *
 * <p>Asks the server to flatten the crafting grid. Servers that ship CraftingTweaks already have
 * their own packet, so we stay out of the way there.
 */
public record BalanceMessage() {

    public static void encode(BalanceMessage message, FriendlyByteBuf buffer) {
        // 空包，什么都不用写。
    }

    public static BalanceMessage decode(FriendlyByteBuf buffer) {
        return new BalanceMessage();
    }

    /** 服务端：有合成辅助就交给它，没有才自己算。 */
    public static void handle(BalanceMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && !ModList.get().isLoaded("craftingtweaks")) {
            AbstractContainerMenu menu = player.containerMenu;
            GridBalance.Result result = GridBalance.balanceMenu(menu);
            if (result != null) {
                menu.broadcastChanges();
                String line = String.format("balance server: %d slots, %d items", result.slots(), result.items());
                UltraStacks.LOGGER.info("[diag] {}", line);
                DiagLog.line("[diag] " + line);
            }
        }
        context.setPacketHandled(true);
    }
}
