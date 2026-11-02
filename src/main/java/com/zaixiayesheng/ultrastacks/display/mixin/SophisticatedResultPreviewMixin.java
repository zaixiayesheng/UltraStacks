package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.display.ResultPreview;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 精妙界面自己重写了 {@code render}，不会走到 {@code AbstractContainerScreen.render} 上挂的那轮预览，
 * 这里在它的 {@code render} 收尾再补一遍。
 *
 * <p>Sophisticated screens override {@code render} and never call the vanilla one, so the shared
 * preview pass has to be repeated at the end of their own render.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public abstract class SophisticatedResultPreviewMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"), require = 0)
    private void ultrastacks$drawResultPreview(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
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
            pose.pushPose();
            pose.translate((float) screen.getGuiLeft(), (float) screen.getGuiTop(), 0.0F);
            for (Slot slot : innerSlots) {
                ResultPreview.draw(graphics, screen, slot);
            }
            pose.popPose();
            DiagLog.lineThrottled("prevres.inner." + innerSlots.size(),
                    "[diag] soph inner slots: n=" + innerSlots.size());
        }
        for (Slot slot : screen.getMenu().slots) {
            ResultPreview.draw(graphics, screen, slot);
        }
    }
}
