package com.zaixiayesheng.ultrastacks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /ultrastacks dump}：把执行者整个物品栏（含副手、盔甲）的完整状态写进
 * {@code config/ultrastacks-dump.txt}。用于「两堆东西不能合并」的现场取证：
 * 玩家复现失败的瞬间执行一次，每个槽位的物品 id、数量、堆叠上限、组件补丁以及
 * 完整 NBT（count > 1 的堆叠）全部落盘，之后直接读文件分析，不需要玩家复制聊天框。
 *
 * <p>{@code /ultrastacks dump} writes the executor's whole inventory (including offhand and
 * armor) to {@code config/ultrastacks-dump.txt}. Item id, count, max stack size, component
 * patch and — for stacks with count &gt; 1 — the full NBT of every occupied slot are captured,
 * so a "two stacks refuse to merge" incident can be forensically analysed from the file.</p>
 */
public final class UltraStacksDumpCommand {

    private UltraStacksDumpCommand() {
    }

    /** NeoForge 事件回调：注册 {@code /ultrastacks dump}。 / Registers the command. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ultrastacks")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("dump")
                        .executes(UltraStacksDumpCommand::dumpInventory)));
    }

    private static int dumpInventory(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Inventory inv = player.getInventory();
        NonNullList<ItemStack> items = inv.items;
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StringBuilder out = new StringBuilder(8192);
        out.append("== ultrastacks dump ").append(timestamp)
                .append(" | player ").append(player.getGameProfile().getName())
                .append(" (").append(player.getStringUUID()).append(") ==\n");
        int empty = 0;
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                empty++;
                continue;
            }
            out.append("slot ").append(slot)
                    .append(": ").append(stack.getCount()).append('x')
                    .append(stack.getDescriptionId())
                    .append(" max=").append(stack.getMaxStackSize())
                    .append("\n  patch=").append(stack.getComponentsPatch())
                    .append('\n');
            if (stack.getCount() > 1) {
                net.minecraft.nbt.Tag tag = stack.saveOptional(player.registryAccess());
                out.append("  nbt=").append(tag.toString()).append('\n');
            }
        }
        out.append("== empty slots: ").append(empty).append(" / ").append(items.size())
                .append(" ==\n");
        Path path = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get()
                .resolve("ultrastacks-dump.txt");
        try {
            Files.writeString(path, out.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            source.sendSuccess(() -> Component.literal(
                    "ultrastacks: 物品栏快照已写入 config/ultrastacks-dump.txt"), false);
        } catch (IOException e) {
            source.sendSuccess(() -> Component.literal(
                    "ultrastacks: 写入失败 " + e.getMessage()), false);
        }
        return 1;
    }
}
