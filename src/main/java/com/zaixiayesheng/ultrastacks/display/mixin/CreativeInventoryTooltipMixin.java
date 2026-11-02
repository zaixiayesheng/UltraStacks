package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.display.DisplayEvents;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 创造模式物品栏的数量行。
 *
 * <p>创造界面自己重写了 {@code getTooltipFromContainerItem}，只挂 {@code AbstractContainerScreen} 拦不到
 * 它的覆写，所以这里单独挂一份；挂在 RETURN 上、取消返回值，补出来的行才排在模组名下面，
 * 和别的界面一致。
 *
 * <p>The creative inventory screen overrides {@code getTooltipFromContainerItem}, so a mixin on
 * {@code AbstractContainerScreen} never sees it. This one appends the count line on RETURN, which keeps
 * the line below the mod name exactly like every other screen.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryTooltipMixin {

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$appendCountLine(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> lines = cir.getReturnValue();
        if (lines == null) {
            return;
        }
        List<Component> updated = DisplayEvents.withCountLine(lines, stack,
                (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) (Object) this);
        if (updated != lines) {
            cir.setReturnValue(updated);
        }
    }
}
