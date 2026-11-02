package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.display.ResultPreview;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 精妙界面自己重写了绘制，不会走到 {@code AbstractContainerScreen#extractSlot} 上挂的那轮预览，
 * 这里在它的 {@code extractContents} 收尾再补一遍。
 *
 * <p>26.1.2 的精妙把 {@code render} 拆成了 {@code extractBackground} / {@code extractContents}，
 * 槽位级的 {@code renderSlot} 也改名 {@code extractSlot}；收尾点用 {@code extractContents} 的 RETURN。
 *
 * <p>Sophisticated screens override the drawing path and never reach the vanilla per-slot preview,
 * so the shared pass is repeated at the end of their own {@code extractContents}.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public abstract class SophisticatedResultPreviewMixin {

    @Inject(method = "extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("RETURN"), require = 0)
    private void ultrastacks$drawResultPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (screen.getMenu() == null) {
            return;
        }
        ResultPreview.drawSophisticatedAnvil(graphics, screen);
        ResultPreview.drawSophisticatedCrafting(graphics, screen);
        // 铁砧页的三个槽在内层容器里，外层菜单扫不到；这里直接遍历内层，坐标是页面相对的，
        // 所以整段平移 guiLeft/guiTop 后再画。
        // The anvil tab keeps its three slots inside the tab container, not in the menu; iterate them
        // directly and shift by guiLeft/guiTop because those coordinates are page-relative.
        var innerSlots = ResultPreview.sophInnerSlots(screen, "upgrades.anvil.AnvilUpgradeContainer");
        if (!innerSlots.isEmpty()) {
            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate((float) screen.getGuiLeft(), (float) screen.getGuiTop());
            for (Slot slot : innerSlots) {
                ResultPreview.draw(graphics, screen, slot);
            }
            pose.popMatrix();
            DiagLog.lineThrottled("prevres.inner." + innerSlots.size(),
                    "[diag] soph inner slots: n=" + innerSlots.size());
        }
        for (Slot slot : screen.getMenu().slots) {
            ResultPreview.draw(graphics, screen, slot);
        }
    }
}
