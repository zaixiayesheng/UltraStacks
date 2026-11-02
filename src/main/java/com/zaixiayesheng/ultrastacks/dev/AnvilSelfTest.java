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

        player.getAbilities().instabuild = true;
        menu.createResult();
        ItemStack out2 = menu.getSlot(2).getItem();
        DiagLog.line("[diag] anvil test(creative): cost=" + menu.getCost()
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
        batch.clicked(2, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
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
        // 连续两次取走：左 4 右 4，点一次取一件，再点一次——红叉/卡死的复现点。
        player.getInventory().clearContent();
        player.experienceLevel = 1000;
        com.zaixiayesheng.ultrastacks.crafting.AnvilBatchGuard.clear(player.getUUID());
        AnvilMenu twice = new AnvilMenu(5, player.getInventory(), ContainerLevelAccess.NULL);
        twice.getSlot(0).set(book(unbreaking, 2, mending, 1).copyWithCount(4));
        twice.getSlot(1).set(book(unbreaking, 2, mending, 1).copyWithCount(4));
        twice.createResult();
        for (int n = 1; n <= 2; n++) {
            twice.clicked(2, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
            DiagLog.line("[diag] anvil twice #" + n
                    + ": left=" + twice.getSlot(0).getItem().getCount()
                    + " right=" + twice.getSlot(1).getItem().getCount()
                    + " result=" + twice.getSlot(2).getItem().getCount()
                    + " cost=" + twice.getCost());
        }

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
