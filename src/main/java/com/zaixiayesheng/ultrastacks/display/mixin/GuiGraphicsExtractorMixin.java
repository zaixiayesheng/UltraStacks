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
 * Display module: the number drawn on a slot (Minecraft 26.1.2).
 *
 * <p>Vanilla draws the full count in {@code itemCount}. COMPACT mode substitutes a shorter string
 * ("65k", "1.5m") at the exact place vanilla was about to draw, shifting the anchor by the width
 * difference so the text keeps vanilla's right alignment. EXACT mode passes the vanilla string
 * straight through.
 *
 * <p>Nothing else is touched: the durability bar and the cooldown overlay are drawn elsewhere and
 * keep working untouched.
 *
 * <p><b>中文</b> 显示模块：槽位上的数字。COMPACT 模式在原版 {@code itemCount} 即将绘制的位置换成缩写
 * （"65k"、"1.5m"），并按字宽差修正落点以保持原版的右对齐；EXACT 模式直接把原版字符串传回去。
 * 耐久条与冷却由别的方法绘制，完全不受影响。
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
