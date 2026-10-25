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
 * 显示模块之二：物品提示框。
 *
 * <p>槽位里画的是缩写后的数字，看不到准确值，所以提示框补上一行「准确数量 + 当前上限」。只有数量超过
 * 99 时才加，普通物品的提示框保持原样；显示模块或提示框开关关闭时整段逻辑不执行。
 *
 * <p>Display module, part two: the item tooltip.
 *
 * <p>The slot itself draws the abbreviated number, so the exact value is out of sight; the tooltip
 * adds a line with the exact count and the current stack limit. It is only added for counts above 99,
 * which leaves ordinary items untouched, and nothing runs at all when the display module or the
 * tooltip switch is off.
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
