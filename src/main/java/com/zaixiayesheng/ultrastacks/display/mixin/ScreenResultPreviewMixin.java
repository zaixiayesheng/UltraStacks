package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.display.ResultPreview;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 整屏画完后再补一轮「这批能做多少」预览。
 *
 * <p>各个模组的界面画槽位的路径各不相同（原版走 {@code extractSlot}，精妙自己的容器另有实现，
 * AE 干脆自己画），逐个挂既容易漏、也会重复。这里统一在
 * {@code AbstractContainerScreen#extractRenderState} 收尾时遍历一遍菜单里的槽位，由
 * {@link ResultPreview} 自己判断哪些是铁砧/合成产物、该不该显示。
 *
 * <p>26.1.2 没有 {@code render}：渲染被拆成「收集绘制指令」（{@code extractRenderState} →
 * {@code extractContents} / {@code extractCarriedItem} / {@code extractTooltip}）再统一提交，
 * 所以收尾点改成 {@code extractRenderState} 的 RETURN。
 *
 * <p>精妙界面另由 {@link SophisticatedResultPreviewMixin} 在它自己的 {@code extractContents}
 * 收尾补画（还多画一层不在菜单里的升级容器槽位），这里跳过它们，免得同一个数字叠画两遍。
 *
 * <p>One pass after the whole screen has been collected: every mod draws its slots its own way, so
 * the preview is drawn once over the finished frame instead of being hooked into each of those
 * paths. 26.1.2 has no {@code render}; the collection root is {@code extractRenderState}.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ScreenResultPreviewMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("RETURN"), require = 0)
    private void ultrastacks$drawResultPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (screen.getMenu() == null) {
            return;
        }
        // 精妙自己那条路在 SophisticatedResultPreviewMixin 里，两处都画会叠成两层字。
        // Sophisticated screens take their own path below; drawing both would double the number.
        if (screen.getClass().getName().startsWith("net.p3pp3rf1y.")) {
            return;
        }
        for (Slot slot : screen.getMenu().slots) {
            ResultPreview.draw(graphics, screen, slot);
        }
    }
}
