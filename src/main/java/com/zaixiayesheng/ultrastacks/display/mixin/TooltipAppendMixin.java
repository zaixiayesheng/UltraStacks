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
 * 在槽位提示框构建完成时补上数量行，位置在模组名之后。
 *
 * <p>模组名是在物品提示框事件之后追加的，只有在这一步追加，这一行才会排在它下面。
 *
 * <p>Adds the count line once the slot tooltip has been built, which places it after the mod name.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class TooltipAppendMixin {

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$appendCountLine(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        cir.setReturnValue(DisplayEvents.withCountLine(cir.getReturnValue(), stack,
                (AbstractContainerScreen<?>) (Object) this));
    }
}
