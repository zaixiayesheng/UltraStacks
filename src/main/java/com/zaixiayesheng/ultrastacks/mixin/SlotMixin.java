package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 槽位上限。两层：一是把容器返回的原版 99 换成配置上限；二是保证「本槽的上限不小于它当前装着的数量」。
 *
 * <p>第二层是为了堵住合并算术里的「空余空间 = 上限 - 已有数量」。当槽里堆着 21 亿、而槽位上限被算成 64 时，
 * 空余空间变成负数，{@code Slot#safeInsert} 拿着负数去做 split / shrink，一边变少一边变多。
 *
 * <p><b>EN</b> Raises slot limits to the configured value and keeps them at least as large as the stack inside.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @ModifyReturnValue(method = "getMaxStackSize()I", at = @At("RETURN"))
    private int ultrastacks$raiseVanillaLimit(int original) {
        int raised = UltraStacksConfig.raiseVanillaDefault(original);
        ItemStack current = ((Slot) (Object) this).getItem();
        return current.isEmpty() ? raised : Math.max(raised, current.getCount());
    }

    @ModifyReturnValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private int ultrastacks$neverBelowContents(int original, ItemStack stack) {
        // 部分原版槽位（酿造台的燃料槽、药水槽）自己重写了无参版本并直接返回 64/1，绕过了上面那个注入。
        int raised = UltraStacksConfig.raiseSlotLimit(original);
        ItemStack current = ((Slot) (Object) this).getItem();
        // 只对「同一种物品」兜底：槽里堆着 1,000 个铁锭时，不能因此让 16 个一组的鸡蛋也塞到 1,000。
        if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, stack)) {
            return raised;
        }
        return Math.max(raised, current.getCount());
    }
}
