package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.display.CountFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 计数文本的显示改造。
 *
 * <p>把 {@code itemCount} 里绘制数量数字的那次 {@code text(...)} 调用重定向到本地方法：数量超过 99、
 * 且原版没有给出替代文本（{@code countText} 为 null）时，改成画
 * {@link com.zaixiayesheng.ultrastacks.display.CountFormat#format} 的结果。剩下两种情况原样转发：数字
 * 被缩写后变短了，用两者字宽之差把落点右移，才能保住原版的右对齐；只有真的换了文本才做这一步。
 *
 * <p>Override for the stack count text.
 *
 * <p>The {@code text(...)} call that draws the count inside {@code itemCount} is redirected to a local
 * method: when the count is above 99 and vanilla has not supplied its own text ({@code countText} is
 * null), the string drawn is {@link com.zaixiayesheng.ultrastacks.display.CountFormat#format}'s result.
 * Two cases are forwarded unchanged — an abbreviated number is shorter than the full one, so the draw
 * position is shifted right by the difference between the two string widths to keep vanilla's right
 * alignment, and that shift only happens when the text was actually replaced.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {

    @Redirect(method = "itemCount", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"
    ))
    private void ultrastacks$compactCount(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color,
                                          boolean dropShadow,
                                          Font itemFont, ItemStack stack, int itemX, int itemY,
                                          @Nullable String countText) {
        if (!UltraStacksConfig.displayModule() || countText != null || text == null || stack.getCount() <= 99) {
            graphics.text(font, text, x, y, color, dropShadow);
            return;
        }

        String shown = CountFormat.format(stack.getCount());
        if (shown.isEmpty()) {
            graphics.text(font, text, x, y, color, dropShadow);
            return;
        }

        int shift = font.width(text) - font.width(shown);
        graphics.text(font, shown, x + shift, y, color, dropShadow);
    }
}
