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

import java.util.List;

/**
 * 精妙界面自己重写了 {@code render}，不会走到 {@code AbstractContainerScreen.render} 上挂的那轮预览，
 * 这里在它的 {@code render} 收尾再补一遍。
 *
 * <p>1.20.1 的模组 jar 里 {@code render(GuiGraphics, int, int, float)} 带的是 SRG 名 {@code m_88315_}，
 * 所以这里写字面名 + {@code remap = false}。
 *
 * <p>Sophisticated screens override {@code render} and never call the vanilla one, so the shared preview
 * pass is repeated at the end of their own render. In the 1.20.1 production jar the override carries the
 * SRG name {@code m_88315_}.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public abstract class SophisticatedResultPreviewMixin {

    @Inject(method = "m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"), require = 0)
    private void ultrastacks$drawResultPreview(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (screen.getMenu() == null) {
            return;
        }
        ResultPreview.drawSophisticatedAnvil(graphics, screen);
        ResultPreview.drawSophisticatedCrafting(graphics, screen);
        // 铁砧页的三个槽在内层容器里，外层菜单扫不到；坐标是页面相对的，所以整段平移 guiLeft/guiTop 后再画。
        List<Slot> innerSlots = ResultPreview.sophInnerSlots(screen, "upgrades.anvil.AnvilUpgradeContainer");
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
