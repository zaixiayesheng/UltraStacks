package com.zaixiayesheng.ultrastacks;

import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * 在注册阶段把配置的堆叠上限写进物品的默认组件。
     *
 * <p>Writes the configured stack limit into the item default components during registration.
 */
public final class ModBusEvents {
    private ModBusEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        int[] changed = {0};
        event.getAllItems().forEach(item -> {
            // 带耐久或可损坏的物品（工具、盔甲）保持原版；上限为 1 的物品由下面的相等判断跳过。
            if (item.components().has(DataComponents.DAMAGE)
                    || item.components().has(DataComponents.MAX_DAMAGE)) {
                return;
            }
            // 桶类单独由 bucketStackLimit 决定：把它们的默认值烘成 16 的倍数，会让配置里的 16/32
            // 全部失效（limitFor 对「非原版默认值」直接原样返回）。
            // Buckets are driven by bucketStackLimit alone; baking a scaled default here would make
            // every configured bucket value unreachable.
            if (item instanceof net.minecraft.world.item.BucketItem
                    || item instanceof net.minecraft.world.item.MilkBucketItem) {
                return;
            }

            // 必须读组件里的原始值：Item#getDefaultMaxStackSize() 已被 ItemDefaultMaxStackMixin 抬高，
            // 从那里读得到的就是新上限，scaledMax() 与原值相等，组件反而永远不会被改写。
            int vanillaMax = item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
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
