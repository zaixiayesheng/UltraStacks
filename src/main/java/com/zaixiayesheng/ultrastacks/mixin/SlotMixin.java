package com.zaixiayesheng.ultrastacks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 槽位上限两层处理：把原版默认值换成配置上限，并保证上限不小于槽里已经装着的数量。
 *
 * <p>无参的 {@code getMaxStackSize()} 只把 99（{@link net.minecraft.world.Container} 的默认值）换成配置
 * 上限，其余数值原样返回，所以本来就只能放 1 个的槽位不受影响。
 *
 * <p>带堆叠参数的版本把 64 与 99 都当成「原版默认大小」换成配置上限，再对同一种物品抬到不小于槽内数量。
 * 后一步不能省：原版把空余空间算成 {@code 上限 - 数量}，上限小于数量时差值为负，{@code safeInsert} 拿着
 * 负数去做拆分与 {@code shrink}，结果一边变少一边变多。兜底只对同一种物品生效——槽里堆着一千个铁锭，不会
 * 因此让十六个一组的鸡蛋也塞到一千。
 *
 * <p>两个注入点都要写：部分原版槽位（酿造台的燃料槽、药水槽）自己重写了无参版本并直接返回 64 或 1，绕过
 * 了第一个注入点。
 *
 * <p>Two layers of slot limiting: vanilla defaults are replaced with the configured stack limit, and a
 * limit is never reported below the number of items already sitting in the slot.
 *
 * <p>The no-argument {@code getMaxStackSize()} only replaces 99 (the default of
 * {@link net.minecraft.world.Container}) with the configured limit and returns every other value
 * unchanged, so slots that genuinely hold a single item are unaffected.
 *
 * <p>The overload that takes a stack treats both 64 and 99 as "the vanilla default size" and then, for the
 * same item, raises the result to at least the count in the slot. That second step is essential: vanilla
 * computes free headroom as {@code limit - count}, so a limit below the count yields a negative number,
 * and {@code safeInsert} passes that negative value into a split and a {@code shrink}, shrinking one side
 * while growing the other. The floor only applies to the same item — a slot holding a thousand iron ingots
 * does not let a sixteen-per-stack egg reach a thousand.
 *
 * <p>Both injection points are needed: some vanilla slots (the brewing stand's fuel and potion slots)
 * override the no-argument version and return 64 or 1 directly, bypassing the first hook.
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
