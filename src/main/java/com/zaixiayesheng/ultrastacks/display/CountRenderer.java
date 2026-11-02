package com.zaixiayesheng.ultrastacks.display;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 画槽位里的数量文本。为了一排数字看起来一致，缩写数字与未缩写数字走**完全相同的几何**：
 * 都用原版的「右下角对齐」公式，只统一上抬 {@link #LIFT} 像素。
 *
 * <p>以前这里照抄了精妙的缩放写法（字宽超过 16 像素就整体缩小并补一个纵向补偿项），结果缩写数字
 * 和 1–99 的数字基线差了半行，一排里一个居中一个偏下。现在不缩放——精妙自己也是原尺寸画、往左
 * 溢出，保持一致比塞进格子更重要。
 *
 * <p>Draws a slot's count. Abbreviated and plain numbers share **one** geometry: vanilla's
 * bottom-right alignment, lifted by {@link #LIFT}. It used to copy Sophisticated's scaling (shrink
 * when wider than 16 px plus a vertical correction), which left abbreviated and 1–99 counts on
 * different baselines. No scaling now — Sophisticated overflows to the left at full size too, and
 * matching it beats cramming into the cell.
 */
public final class CountRenderer {

    /** 槽位留给数量的宽度。 / Width a slot leaves for its count. */
    public static final int SLOT_TEXT_WIDTH = 16;

    /** 最宽缩写的像素宽（65.5k = 25），用来统一字号。 / Width of the widest abbreviation, used to unify size. */
    private static final int REFERENCE_WIDTH = 25;

    /**
     * 相对原版坐标上抬的像素数：原版数量贴着格子下沿、会压住 GUI 边框一像素。
     *
     * <p>Pixels lifted above vanilla's baseline: vanilla's count hugs the slot's bottom edge and
     * clips the GUI border by a pixel.
     */
    public static final int LIFT = 0;

    /** 与格子边缘之间保留的像素。 / Padding kept from the cell's edges. */
    public static final int PAD = 1;

    private CountRenderer() {
    }

    /**
     * 在槽位左上角为 {@code (slotX, slotY)} 的格子里画数量，几何与未缩写数字完全一致。
     *
     * <p>Draws the count inside the slot whose top-left corner is {@code (slotX, slotY)}, using the
     * same geometry as plain counts.
     */
    public static void drawForSlot(GuiGraphics graphics, Font font, String text, int slotX, int slotY) {
        // 未缩写的数字由原版画在 (slotX+17-width, slotY+9)，字身 8 像素高，底边落在 slotY+17，
        // 那 1 像素本身就压在 GUI 底边上。缩写若对齐到 17 就一样会盖住，所以整条底边往上抬 1 像素到 16。
        // 缩写要让「底边」和它完全一致，所以锚点取右下角 (slotX+17, slotY+17)，缩放只影响字身高度。
        // Vanilla draws the plain count at (slotX+17-width, slotY+9); the glyph box is 8 px tall, so its
        // bottom edge is slotY+17. The abbreviation anchors to that same bottom-right corner, so scaling
        // only changes the glyph height and never pushes the text past the plain count.
        int width = font.width(text);
        // 统一字号：所有缩写都按「最宽的那种缩写」（65.5k，25 像素）缩放，这样同一排里
        // 131k 和 65.5k 一样大，也不会因为 4 字符时几乎不缩放而顶出格子。
        // One size for every abbreviation: scale against the widest abbreviation we ever emit
        // ("65.5k", 25 px) so 131k and 65.5k read at the same size and never exceed the cell.
        float scale = Math.min(1.0F, 16.0F / (float) width);
        if (scale >= 1.0F) {
            graphics.drawString(font, text, slotX + 17 - width, slotY + 9, 0xFFFFFFFF, true);
            return;
        }
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate((float) (slotX + 17), (float) (slotY + 16), 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -width, -8, 0xFFFFFFFF, true);
        pose.popPose();
    }

    /**
     * 在调用方已经算好的文本坐标 {@code (textX, textY)} 处画数量，不做任何位移。
     * 给 AE2 那些自己定位文本的注入点用。
     *
     * <p>Draws the count at a text position the caller already computed, with no shift. Used by the
     * AE2 hooks, which position the label themselves.
     */
    public static void drawAt(GuiGraphics graphics, Font font, String text, int textX, int textY) {
        graphics.drawString(font, text, textX, textY, 0xFFFFFFFF, true);
    }
}
