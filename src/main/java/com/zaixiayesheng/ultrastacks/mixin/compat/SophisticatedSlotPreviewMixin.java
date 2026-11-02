package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 拖拽分配时，空格子上的预览数字不再写死 64。
 *
 * <p>精妙的界面在画「拖着的这一摞要分多少」时，目标格为空就直接用常量 64 当上限，非空才去问
 * {@code slot.getMaxStackSize(stack)}。于是拖到 8 个空格子上时每格都显示 64，松手后服务端按真实上限
 * 结算，数字才对得上。这里只把这一个常量换成配置值，不改它的任何判定 —— 预览看到多少，松手就是多少。
 *
 * <p>Sophisticated's screen draws the per-slot preview of a dragged stack with a hard-coded 64 when the
 * target slot is empty, and only asks {@code slot.getMaxStackSize(stack)} when it is not. Dragging onto
 * eight empty cells therefore previews 64 each, while the server splits by the real limit on release.
 * Only that one constant is replaced with the configured value; none of Sophisticated's decisions change.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public abstract class SophisticatedSlotPreviewMixin {

    @ModifyConstant(method = "renderSlot", constant = @Constant(intValue = 64), require = 0)
    private int ultrastacks$raiseEmptySlotPreview(int original) {
        return Math.max(original, UltraStacksConfig.limitCap());
    }
}
