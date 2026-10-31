package com.zaixiayesheng.ultrastacks.display;

import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 显示模块之二：物品提示框。
     *
 * <p>Display module, part two: the item tooltip. The slot itself shows an abbreviated number, so the
 * tooltip carries the exact count and the current stack limit. Nothing is added for counts of 99 or
 * less, which keeps ordinary items untouched, and either switch (display or tooltipExact) turns the
 * whole thing off.
 */
@Mod.EventBusSubscriber(modid = UltraStacks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
