/*
 * 中文：26.1.2 的 StorageScreenBase 把槽位绘制拆成了 extractSlot（原 renderSlot）。
 * 拖拽分配时，空格子的预览数字仍是写死的 64，于是「拖到 8 个空格」只预览 64。
 * 这里只替换那一个常量，不动布局，和 1.21.1 上 renderSlot 的处理保持一致。
 * 用核心 Mixin 的 @ModifyConstant（26.1.2 的编译类路径上没有 MixinExtras）。
 *
 * English: 26.1.2 split slot rendering into extractSlot (formerly renderSlot).
 * The drag-preview number for an empty target slot is still the hardcoded 64,
 * so dragging across empty slots previews 64 per slot. Only that constant is
 * replaced here, matching the 1.21.1 tree. Core @ModifyConstant is used because
 * this tree's compile classpath has no MixinExtras.
 */
package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public class SophisticatedSlotPreviewMixin {

    @ModifyConstant(method = "extractSlot", constant = @Constant(intValue = 64), require = 0)
    private int ultrastacks$raiseEmptySlotPreview(int original) {
        return Math.max(original, UltraStacksConfig.limitCap());
    }
}
