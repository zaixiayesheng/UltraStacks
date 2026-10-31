package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 显示模块的另一半：物品提示框。
     *
 * <p>The other half of the display module: the item tooltip. Slot counts are abbreviated to
 * k / m / b, so the tooltip supplies the exact count together with the current stack limit and
 * no information is lost. The line is only added when both the display module and the tooltip
 * switch are on and the count is above 99, which leaves ordinary item tooltips untouched. It is
 * registered on the client only.
 */
@EventBusSubscriber(modid = UltraStacks.MOD_ID, value = Dist.CLIENT)
public final class DisplayEvents {

    private DisplayEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!UltraStacksConfig.displayModule() || !UltraStacksConfig.tooltipExact()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.getCount() <= 99) {
            return;
        }
        event.getToolTip().add(Component.translatable("ultrastacks.tooltip.count",
                stack.getCount(), stack.getMaxStackSize()).withStyle(ChatFormatting.GRAY));
    }
}
