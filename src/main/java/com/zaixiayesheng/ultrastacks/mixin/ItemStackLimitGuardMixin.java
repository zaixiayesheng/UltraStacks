package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacks;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 不许把「历史上超限的堆叠」静默截断。
 *
 * <p>{@code ItemStack#limitSize(int)} 是各处容器与模组用来「夹到上限」的公共出口：背包、菜单、存档
 * 读回、整理逻辑都会调它。上限被夹成配置值以后，一个 60 万的存量堆叠从这些地方经过时会被一刀砍成
 * 65536 —— 数量对不上，东西就这么没了。这里直接放弃这次截断：这类堆叠进不了任何槽位（插入路径已经
 * 被挡住），玩家把它拖进空格子时才会按上限自然拆成多堆，全程一个不少。
 *
 * <p>Never truncate a legacy oversized stack silently. {@code ItemStack#limitSize(int)} is the common
 * "clamp to the limit" exit used by containers, menus, loading and sorting code; once the limit is the
 * configured value, a 600k stack passing through it would be cut to 65536 and the difference is simply
 * gone. This guard cancels that truncation instead: such a stack cannot enter any slot anyway (insertion
 * is blocked), and dragging it into empty space splits it into legal stacks without losing anything.</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackLimitGuardMixin {

    @Inject(method = "limitSize(I)V", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$keepOversizedStack(int maxCount, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        int cap = UltraStacksConfig.limitCap();
        if (self.getCount() <= cap || maxCount > cap) {
            return;
        }
        // 26.1.2 删了 ItemStack#getDescriptionId()，物品 id 只能从 getItem() 上取。
        // 26.1.2 dropped ItemStack#getDescriptionId(); the item id now comes from getItem().
        String line = "[diag] limitSize blocked: " + self.getItem().getDescriptionId() + " x" + self.getCount()
                + " would be cut to " + maxCount;
        UltraStacks.LOGGER.debug(line);
        DiagLog.line(line);
        ci.cancel();
    }
}
