package com.zaixiayesheng.ultrastacks.display.mixin;

import java.util.List;

import com.zaixiayesheng.ultrastacks.display.DisplayEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙自己重写了槽位提示框的构建方法，上一条注入到 {@code AbstractContainerScreen} 的规则不会进到这里，
 * 所以这里单独再挂一次。精妙会自己写数量行时（装了解锁槽位上限的升级）由 {@code DisplayEvents} 放行。
 *
 * <p>1.20.1 的模组 jar 里这个覆写带的是 SRG 名 {@code m_280553_}（即
 * {@code AbstractContainerScreen.getTooltipFromContainerItem}），和 {@code m_6893_} 那些覆写一样，
 * 所以这里用字面名 + {@code remap = false} 指向它。
 *
 * <p>Sophisticated overrides the slot tooltip builder, so the injection on
 * {@code AbstractContainerScreen} never runs inside its screens; this mirror adds the line there and
 * steps aside when Sophisticated draws the line itself. In the 1.20.1 production jar the override
 * carries the SRG name {@code m_280553_}, so it is named literally with {@code remap = false}.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public abstract class SophisticatedTooltipMixin {

    @Inject(
            method = "m_280553_(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0)
    private void ultrastacks$appendCountLine(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> lines = cir.getReturnValue();
        if (lines == null) {
            return;
        }
        List<Component> updated = DisplayEvents.withCountLine(lines, stack,
                (AbstractContainerScreen<?>) (Object) this);
        if (updated != lines) {
            cir.setReturnValue(updated);
        }
    }
}
