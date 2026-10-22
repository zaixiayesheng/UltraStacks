package com.zaixiayesheng.ultrastacks;

import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Applies the configured stack limit to item defaults at registration time.
 *
 * <p><b>中文</b> 在注册阶段把配置上限写进物品默认值，跳过带耐久的物品。
 */
public final class ModBusEvents {
    private ModBusEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        int[] changed = {0};
        event.getAllItems().forEach(item -> {
            // Damaged or damaged-capable items (tools, armour) and single-stack items stay vanilla.
            if (item.components().has(DataComponents.DAMAGE)
                    || item.components().has(DataComponents.MAX_DAMAGE)) {
                return;
            }

            int vanillaMax = item.getDefaultMaxStackSize();
            int newMax = UltraStacksConfig.scaledMax(vanillaMax);
            if (newMax == vanillaMax) {
                return;
            }

            event.modify(item, builder -> builder.set(DataComponents.MAX_STACK_SIZE, newMax));
            changed[0]++;
        });

        if (changed[0] == 0) {
            UltraStacks.LOGGER.warn("No item stack limit was changed; check maxStackSize (currently {}).",
                    UltraStacksConfig.maxStackSize());
        } else {
            UltraStacks.LOGGER.info("Applied stack limit {} to {} items.", UltraStacksConfig.maxStackSize(), changed[0]);
        }
    }
}
