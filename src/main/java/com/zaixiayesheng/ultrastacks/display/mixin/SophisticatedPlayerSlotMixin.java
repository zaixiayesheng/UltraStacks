package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.DiagLog;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 精妙界面自己画数量：只要数量超过 99 就走它自己的缩写（{@code 65.5k}、{@code 33.0k}），
 * 连玩家物品栏那几行也一样——于是同一个 33099 在精妙背包里是 {@code 33.0k}、在外面是 {@code 33.1k}。
 *
 * <p>这里只接管**容器是玩家背包**的那些槽位：精妙的网格保持原样由它自己画，玩家物品栏改回我们的
 * 格式（四舍五入、位数固定），这样开着背包和关掉背包看到的是同一个数字、同一个字号。
 *
 * <p>Sophisticated draws its own counts — anything above 99 goes through its abbreviator
 * ({@code 65.5k}, {@code 33.0k}), player inventory rows included, so one stack reads
 * {@code 33.0k} inside a backpack and {@code 33.1k} outside. This takes over only the slots whose
 * container is the player inventory; the backpack grid keeps drawing itself, while the item rows go
 * back to our format (rounded, fixed width) so the number and its size stay the same with or without
 * the screen open.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public abstract class SophisticatedPlayerSlotMixin {

    @Inject(
            method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void ultrastacks$drawPlayerSlotsOurselves(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (!UltraStacksConfig.displayModule()) {
            return;
        }
        // 精妙界面不走 AbstractContainerScreen.renderSlot，产物预览得在这里补一次。
        // These screens do not go through AbstractContainerScreen.renderSlot, so the result preview is
        // drawn here as well.
        // 玩家物品栏与精妙自己的存储格都接管：两处都要同一套缩写与同一套字号，
        // 否则背包上半部分（精妙按字宽缩放）和下半部分（我们按固定字号缩放）会一大一小。
        // Player rows and Sophisticated's own storage cells are both taken over so the grid and the
        // inventory read at one size; otherwise the top half (scaled per-string by Sophisticated) and
        // the bottom half (our fixed scale) differ.
        boolean ownStorageCell = slot.getClass().getName()
                .startsWith("net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot");
        if (!(slot.container instanceof Inventory) && !ownStorageCell) {
            return;
        }
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }
        // 精妙界面的槽位坐标已经是绝对屏幕坐标，直接照抄即可。
        // Slot coordinates in these screens are already absolute, so they are used as-is.
        graphics.renderItem(stack, slot.x, slot.y);
        // 传 null：让原版自己算出数量文本，再交给挂在 GuiGraphics 上的缩写逻辑。
        // null lets vanilla build the count text, which then goes through our abbreviation hook.
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, slot.x, slot.y, null);
        ci.cancel();
    }
}
