package com.zaixiayesheng.ultrastacks.dev;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 只在开发环境用的铁砧自测：不点界面，直接把两本附魔书塞进 {@link AnvilMenu} 算一次结果。
 *
 * <p>Dev-only anvil self test: it places two enchanted books into an {@link AnvilMenu} and runs the
 * result computation directly, so the survival-only branch can be inspected without a client. It
 * runs only when a marker file named {@code anviltest.flag} exists in the working directory and
 * shuts the server down afterwards.
 */
@EventBusSubscriber(modid = UltraStacks.MOD_ID)
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
        System.out.println("[diag] anvil test threw " + t);
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

        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> unbreaking = registry.getOrThrow(Enchantments.UNBREAKING);
        Holder<Enchantment> mending = registry.getOrThrow(Enchantments.MENDING);

        ItemStack left = book(unbreaking, 2, mending, 1);
        ItemStack right = book(unbreaking, 2, mending, 1);

        DiagLog.line("[diag] probe: supportsEnchantment(unbreaking)=" + left.supportsEnchantment(unbreaking)
                + " bookEnchantable=" + left.isBookEnchantable(right)
                + " stored=" + right.has(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS)
                + " instabuild=" + player.getAbilities().instabuild
                + " level=" + player.experienceLevel);
        System.out.println("[diag] probe: supportsEnchantment(unbreaking)=" + left.supportsEnchantment(unbreaking)
                + " bookEnchantable=" + left.isBookEnchantable(right)
                + " stored=" + right.has(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS)
                + " instabuild=" + player.getAbilities().instabuild
                + " level=" + player.experienceLevel);

        ItemStack mergedProbe = left.copy();
        var m2 = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        m2.set(unbreaking, 3);
        m2.set(mending, 1);
        EnchantmentHelper.setEnchantments(mergedProbe, m2.toImmutable());
        DiagLog.line("[diag] probe2: mergedBookEnchantable=" + mergedProbe.isBookEnchantable(right)
                + " mergedIsBook=" + mergedProbe.is(Items.ENCHANTED_BOOK));
        System.out.println("[diag] probe2: mergedBookEnchantable=" + mergedProbe.isBookEnchantable(right)
                + " mergedIsBook=" + mergedProbe.is(Items.ENCHANTED_BOOK));

        AnvilMenu menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(left);
        menu.getSlot(1).set(right);
        menu.createResult();

        ItemStack out = menu.getSlot(2).getItem();
        DiagLog.line("[diag] anvil test(survival): left=" + menu.getSlot(0).getItem().getCount()
                + " right=" + menu.getSlot(1).getItem().getCount()
                + " cost=" + menu.getCost()
                + " out=" + out.getCount()
                + " ench=" + EnchantmentHelper.getEnchantmentsForCrafting(out));
        System.out.println("[diag] anvil test(survival): left=" + menu.getSlot(0).getItem().getCount()
                + " right=" + menu.getSlot(1).getItem().getCount()
                + " cost=" + menu.getCost()
                + " out=" + out.getCount()
                + " ench=" + EnchantmentHelper.getEnchantmentsForCrafting(out));

        // 重命名：一次一件、每件扣 1 级、左槽只少一个。 / Rename: one item per take.
        {
            player.experienceLevel = 5;
            AnvilMenu rn = new AnvilMenu(3, player.getInventory(), ContainerLevelAccess.NULL);
            rn.getSlot(0).set(new ItemStack(Items.DIRT, 4));
            rn.setItemName("Renamed");
            rn.createResult();
            DiagLog.line("[diag] anvil rename before: left=" + rn.getSlot(0).getItem().getCount()
                    + " out=" + rn.getSlot(2).getItem().getCount()
                    + " cost=" + rn.getCost()
                    + " name=" + rn.getSlot(2).getItem().getHoverName().getString());
            System.out.println("[diag] anvil rename before: left=" + rn.getSlot(0).getItem().getCount()
                    + " out=" + rn.getSlot(2).getItem().getCount() + " cost=" + rn.getCost());
            // 模拟“鼠标点一下”：直接走槽位取走，不经过 shift 批量。
            // Emulates a single click: take the result through the slot, bypassing the shift batch.
            net.minecraft.world.item.ItemStack clicked = rn.getSlot(2).getItem().copy();
            rn.getSlot(2).onTake(player, clicked);
            rn.createResult();
            DiagLog.line("[diag] anvil rename click1: left=" + rn.getSlot(0).getItem().getCount()
                    + " out=" + rn.getSlot(2).getItem().getCount()
                    + " outName=" + rn.getSlot(2).getItem().getHoverName().getString()
                    + " cost=" + rn.getCost());
            System.out.println("[diag] anvil rename click1: left=" + rn.getSlot(0).getItem().getCount()
                    + " out=" + rn.getSlot(2).getItem().getCount()
                    + " outName=" + rn.getSlot(2).getItem().getHoverName().getString()
                    + " cost=" + rn.getCost());
            try {
                var f = net.minecraft.world.inventory.AnvilMenu.class.getDeclaredField("itemName");
                f.setAccessible(true);
                DiagLog.line("[diag] anvil rename field before: itemName=" + f.get(rn));
                System.out.println("[diag] anvil rename field before: itemName=" + f.get(rn));
                f.set(rn, null);
                f.set(rn, "Renamed");
            } catch (Throwable t) {
                DiagLog.line("[diag] anvil rename field probe failed: " + t);
            }
            rn.quickMoveStack(player, 2);
            DiagLog.line("[diag] anvil rename after1: left=" + rn.getSlot(0).getItem().getCount()
                    + " out=" + rn.getSlot(2).getItem().getCount() + " cost=" + rn.getCost());
            System.out.println("[diag] anvil rename after1: left=" + rn.getSlot(0).getItem().getCount()
                    + " out=" + rn.getSlot(2).getItem().getCount() + " cost=" + rn.getCost());
            rn.createResult();
            DiagLog.line("[diag] anvil rename after2: left=" + rn.getSlot(0).getItem().getCount()
                    + " out=" + rn.getSlot(2).getItem().getCount()
                    + " outName=" + rn.getSlot(2).getItem().getHoverName().getString()
                    + " cost=" + rn.getCost());
            System.out.println("[diag] anvil rename after2: left=" + rn.getSlot(0).getItem().getCount()
                    + " out=" + rn.getSlot(2).getItem().getCount()
                    + " outName=" + rn.getSlot(2).getItem().getHoverName().getString());
        }

        player.getAbilities().instabuild = true;
        menu.createResult();
        ItemStack out2 = menu.getSlot(2).getItem();
        DiagLog.line("[diag] anvil test(creative): cost=" + menu.getCost()
                + " out=" + out2.getCount()
                + " ench=" + EnchantmentHelper.getEnchantmentsForCrafting(out2));
        System.out.println("[diag] anvil test(creative): cost=" + menu.getCost()
                + " out=" + out2.getCount()
                + " ench=" + EnchantmentHelper.getEnchantmentsForCrafting(out2));
        player.getAbilities().instabuild = false;

        // 批量：左槽两百本、右槽两百本，shift 一次应该只做 bulkCraftLimit 件。
        // Batch: 200 books on each side; one shift take must stop at bulkCraftLimit.
        player.getInventory().clearContent();
        player.experienceLevel = 1000;
        AnvilMenu batch = new AnvilMenu(2, player.getInventory(), ContainerLevelAccess.NULL);
        batch.getSlot(0).set(book(unbreaking, 2, mending, 1).copyWithCount(200));
        batch.getSlot(1).set(book(unbreaking, 2, mending, 1).copyWithCount(200));
        batch.createResult();
        int before = batch.getSlot(0).getItem().getCount();
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
                + " limit=" + UltraStacksConfig.bulkCraftLimit());
        System.out.println("[diag] anvil batch: leftBefore=" + before
                + " leftAfter=" + batch.getSlot(0).getItem().getCount()
                + " rightAfter=" + batch.getSlot(1).getItem().getCount()
                + " produced=" + produced
                + " limit=" + UltraStacksConfig.bulkCraftLimit());
    }

    private static ItemStack book(Holder<Enchantment> a, int la, Holder<Enchantment> b, int lb) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        var mutable = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        mutable.set(a, la);
        mutable.set(b, lb);
        EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
        return stack;
    }
}
