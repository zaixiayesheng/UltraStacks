package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.display.CountFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 把槽位里的数量数字换成可读的形式。
 *
 * <p>计数超过 99 且没有自定义文本时接管这一处数字绘制：交给 {@link CountFormat} 决定是原样还是缩写
 * （阈值由 {@code abbreviateAbove} 控制，最大 1000，所以默认配置下 999 及以下仍画完整数字）。
 * 缩写的文本更窄，落点按两者字宽之差右移，右对齐位置保持和原版一致。
 *
 * <p>Replaces the count drawn in a slot with something readable. Above 99 and without custom text this
 * redirect takes over: {@link CountFormat} decides whether the number stays as-is or is abbreviated
 * ({@code abbreviateAbove} caps the threshold at 1000, so by default 999 and below are still drawn in
 * full). The abbreviation is narrower, so it is shifted right by the difference in text width to keep
 * vanilla's right alignment.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Redirect(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
            )
    )
    private int ultrastacks$compactCount(GuiGraphics graphics, Font font, String text, int x, int y, int color,
                                         boolean dropShadow,
                                         Font itemFont, ItemStack stack, int itemX, int itemY,
                                         @Nullable String customText) {
        if (!UltraStacksConfig.displayModule() || customText != null || text == null || stack.getCount() <= 99) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }

        String shown = CountFormat.format(stack.getCount());
        if (shown.isEmpty()) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }

        int shift = font.width(text) - font.width(shown);
        return graphics.drawString(font, shown, x + shift, y, color, dropShadow);
    }
}
