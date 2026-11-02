package com.zaixiayesheng.ultrastacks.display.mixin;

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
 * <p>这里接管**容器是玩家背包**以及精妙自己存储格的槽位，改由 {@link GuiGraphicsMixin} 那套缩写绘制，
 * 这样开着背包和关掉背包看到的是同一个数字、同一个字号。
 *
 * <p>1.20.1 的模组 jar 里 {@code renderSlot} 带的是 SRG 名 {@code m_280092_}，所以这里写字面名 +
 * {@code remap = false}。
 *
 * <p>Sophisticated draws its own counts; this takes over the player-inventory rows and its storage cells
 * so one stack reads the same number and the same size with or without the screen open. In the 1.20.1
 * production jar {@code renderSlot} carries the SRG name {@code m_280092_}.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public abstract class SophisticatedPlayerSlotMixin {

    @Inject(
            method = "m_280092_(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void ultrastacks$drawPlayerSlotsOurselves(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (!UltraStacksConfig.displayModule()) {
            return;
        }
        // 精妙界面不走 AbstractContainerScreen.renderSlot，产物预览得在别处补（见 SophisticatedResultPreviewMixin）。
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
        graphics.renderItem(stack, slot.x, slot.y);
        // 传 null：让原版自己算出数量文本，再交给挂在 GuiGraphics 上的缩写逻辑。
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, slot.x, slot.y, null);
        ci.cancel();
    }
}
