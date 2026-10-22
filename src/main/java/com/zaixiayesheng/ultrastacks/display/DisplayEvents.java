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
 * Display module, part two: the item tooltip (Minecraft 1.20.1 / Forge).
 *
 * <p>Large stacks are shortened in the slot itself, so the tooltip carries the exact number and the
 * current stack limit. It only appears for counts above 99 to keep normal items untouched.
 *
 * <p><b>中文</b> 显示模块之二：物品提示框。槽位里显示缩写数字，提示框补上准确数量与当前上限；
 * 只有数量超过 99 时才添加，普通物品的提示框保持原样。
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
