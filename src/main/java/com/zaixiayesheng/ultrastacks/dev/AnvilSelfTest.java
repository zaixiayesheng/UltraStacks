package com.zaixiayesheng.ultrastacks.dev;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 只在开发环境用的铁砧自测（1.20.1 版）：不点界面，直接把两本附魔书塞进 {@link AnvilMenu} 算一次结果。
 *
 * <p>Dev-only anvil self test for 1.20.1. It runs only when a marker file named
 * {@code anviltest.flag} exists in the working directory and shuts the server down afterwards.
 */
@Mod.EventBusSubscriber(modid = UltraStacks.MOD_ID)
public final class AnvilSelfTest {

    private static final String FLAG = "anviltest.flag";

    private AnvilSelfTest() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Files.exists(Path.of(FLAG))) {
            return;
        }
        MinecraftServer server = event.getServer();
        try {
            run(server);
        } catch (Throwable t) {
            DiagLog.line("[diag] anvil test threw " + t);
        } finally {
            try {
                Files.deleteIfExists(Path.of(FLAG));
            } catch (Exception ignored) {
                // 标记文件删不掉也不影响结果
            }
            server.halt(false);
        }
    }

    private static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(0, 64, 0);
        Enchantment unbreaking = Enchantments.UNBREAKING;
        Enchantment mending = Enchantments.MENDING;

        ItemStack left = book(unbreaking, 2, mending, 1);
        ItemStack right = book(unbreaking, 2, mending, 1);

        AnvilMenu menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(left);
        menu.getSlot(1).set(right);
        menu.createResult();

        ItemStack out = menu.getSlot(2).getItem();
        DiagLog.line("[diag] anvil test(survival): left=" + menu.getSlot(0).getItem().getCount()
                + " right=" + menu.getSlot(1).getItem().getCount()
                + " cost=" + menu.getCost()
                + " out=" + out.getCount()
                + " ench=" + EnchantmentHelper.getEnchantments(out));

        player.getAbilities().instabuild = true;
        menu.createResult();
        ItemStack out2 = menu.getSlot(2).getItem();
        DiagLog.line("[diag] anvil test(creative): cost=" + menu.getCost()
                + " out=" + out2.getCount()
                + " ench=" + EnchantmentHelper.getEnchantments(out2));
        player.getAbilities().instabuild = false;

        // 批量：左槽两百本、右槽两百本，shift 一次应该只做 bulkCraftLimit 件。
        player.getInventory().clearContent();
        player.experienceLevel = 1000;
        AnvilMenu batch = new AnvilMenu(2, player.getInventory(), ContainerLevelAccess.NULL);
        batch.getSlot(0).set(book(unbreaking, 2, mending, 1).copyWithCount(200));
        batch.getSlot(1).set(book(unbreaking, 2, mending, 1).copyWithCount(200));
        batch.createResult();
        int before = batch.getSlot(0).getItem().getCount();
        int costBefore = batch.getCost();
        batch.quickMoveStack(player, 2);
        int produced = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && st.is(Items.ENCHANTED_BOOK)) {
                produced += st.getCount();
            }
        }
        DiagLog.line("[diag] anvil batch: leftBefore=" + before
                + " leftAfter=" + batch.getSlot(0).getItem().getCount()
                + " rightAfter=" + batch.getSlot(1).getItem().getCount()
                + " produced=" + produced
                + " costBefore=" + costBefore
                + " limit=" + UltraStacksConfig.bulkCraftLimit()
                + " result=" + batch.getSlot(2).getItem().getCount());

        // 按住解锁键：同一份材料应该做到上限（4096 内有多少做多少）。
        com.zaixiayesheng.ultrastacks.crafting.UnlockState.set(player.getUUID(), true);
        player.getInventory().clearContent();
        com.zaixiayesheng.ultrastacks.crafting.AnvilBatchGuard.clear(player.getUUID());
        AnvilMenu unlockedRun = new AnvilMenu(4, player.getInventory(), ContainerLevelAccess.NULL);
        unlockedRun.getSlot(0).set(book(unbreaking, 2, mending, 1).copyWithCount(200));
        unlockedRun.getSlot(1).set(book(unbreaking, 2, mending, 1).copyWithCount(200));
        unlockedRun.createResult();
        unlockedRun.quickMoveStack(player, 2);
        int unlockedProduced = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && st.is(Items.ENCHANTED_BOOK)) {
                unlockedProduced += st.getCount();
            }
        }
        DiagLog.line("[diag] anvil unlocked: leftAfter=" + unlockedRun.getSlot(0).getItem().getCount()
                + " produced=" + unlockedProduced);
        com.zaixiayesheng.ultrastacks.crafting.UnlockState.set(player.getUUID(), false);

        // 模拟游戏内 shift 点击产物槽（走 AbstractContainerMenu.clicked → quickMoveStack）
        player.getInventory().clearContent();
        player.experienceLevel = 1000;
        com.zaixiayesheng.ultrastacks.crafting.AnvilBatchGuard.clear(player.getUUID());
        AnvilMenu small = new AnvilMenu(3, player.getInventory(), ContainerLevelAccess.NULL);
        small.getSlot(0).set(book(unbreaking, 2, mending, 1).copyWithCount(2));
        small.getSlot(1).set(book(unbreaking, 2, mending, 1).copyWithCount(2));
        small.createResult();
        int smallCost = small.getCost();
        small.clicked(2, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
        DiagLog.line("[diag] anvil click: cost=" + smallCost
                + " left=" + small.getSlot(0).getItem().getCount()
                + " right=" + small.getSlot(1).getItem().getCount()
                + " result=" + small.getSlot(2).getItem().getCount()
                + " nextCost=" + small.getCost());
        // 连续两次：左 4 右 4，点一次取走一件，再点一次——红叉/卡死的复现点。
        player.getInventory().clearContent();
        player.experienceLevel = 1000;
        com.zaixiayesheng.ultrastacks.crafting.AnvilBatchGuard.clear(player.getUUID());
        AnvilMenu twice = new AnvilMenu(5, player.getInventory(), ContainerLevelAccess.NULL);
        twice.getSlot(0).set(book(unbreaking, 2, mending, 1).copyWithCount(4));
        twice.getSlot(1).set(book(unbreaking, 2, mending, 1).copyWithCount(4));
        twice.createResult();
        for (int n = 1; n <= 2; n++) {
            twice.clicked(2, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            DiagLog.line("[diag] anvil twice #" + n
                    + ": left=" + twice.getSlot(0).getItem().getCount()
                    + " right=" + twice.getSlot(1).getItem().getCount()
                    + " result=" + twice.getSlot(2).getItem().getCount()
                    + " cost=" + twice.getCost());
        }
        renameBatch(server);
    }

    /**
     * 批量重命名：左槽 200 个泥土，右槽空，shift 一次应该做 bulkCraftLimit 次。
     *
     * <p>Batch rename: 200 dirt on the left, empty right slot, one shift should take bulkCraftLimit items.
     */
    private static void renameBatch(MinecraftServer server) {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        player.getInventory().clearContent();
        player.experienceLevel = 1000;
        com.zaixiayesheng.ultrastacks.crafting.UnlockState.set(player.getUUID(), false);
        com.zaixiayesheng.ultrastacks.crafting.AnvilBatchGuard.clear(player.getUUID());
        AnvilMenu menu = new AnvilMenu(9, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(new ItemStack(Items.DIRT, 200));
        menu.setItemName("Renamed");
        menu.createResult();
        int before = menu.getSlot(0).getItem().getCount();
        int outBefore = menu.getSlot(2).getItem().getCount();
        menu.quickMoveStack(player, 2);
        int produced = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && st.is(Items.DIRT)) {
                produced += st.getCount();
            }
        }
        DiagLog.line("[diag] anvil rename: leftBefore=" + before
                + " outBefore=" + outBefore
                + " leftAfter=" + menu.getSlot(0).getItem().getCount()
                + " produced=" + produced
                + " limit=" + UltraStacksConfig.bulkCraftLimit()
                + " result=" + menu.getSlot(2).getItem().getCount());
    }

    private static ItemStack book(Enchantment a, int la, Enchantment b, int lb) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        java.util.Map<Enchantment, Integer> map = new java.util.HashMap<>();
        map.put(a, la);
        map.put(b, lb);
        EnchantmentHelper.setEnchantments(map, stack);
        return stack;
    }
}
