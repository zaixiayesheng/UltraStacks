package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 已经装满的槽位不再接受插入。
 *
 * <p>{@code Slot#safeInsert} 里算可放数量的方式是 {@code 上限 - 槽内数量}，原版默认上限恒为 64、
 * 槽内数量永远不超过 64，所以这个差值不会为负。上限被抬到配置值以后，只要槽里出现一个超过上限的
 * 存量堆叠（旧版本留下的、或其它模组塞进来的），这个差值就变成负数，随后拿去 {@code split} /
 * {@code shrink}：{@code shrink(负数)} 等于增加数量，一次点击就能让物品越拆越多。
 *
 * <p>这里不试图去「修」那个超限的堆叠（那属于 {@code /ultrastacks repair} 的活），只在入口把这次
 * 插入直接挡回去：槽里已经不小于上限时，原样交还传入的堆叠，不改变任何一方的数量。
 *
 * <p>Vanilla {@code Slot#safeInsert} computes the room as {@code limit - count}; the vanilla limit is
 * always 64 and a slot never holds more than that, so the difference is never negative. Once the limit
 * is raised to the configured value, any legacy stack above the limit makes that difference negative,
 * and it is then handed to {@code split} / {@code shrink}: a negative {@code shrink} adds items, so a
 * single click duplicates them. This guard does not try to repair such a stack (that is
 * {@code /ultrastacks repair}'s job); it simply refuses the insert when the slot already holds at
 * least the cap, returning the incoming stack untouched.</p>
 */
@Mixin(Slot.class)
public abstract class SlotGuardMixin {

    @Inject(
            method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true)
    private void ultrastacks$blockInsertIntoFullSlot(ItemStack stack, int amount,
                                                     CallbackInfoReturnable<ItemStack> cir) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = ((Slot) (Object) this).getItem();
        if (current.isEmpty()) {
            return;
        }
        if (current.getCount() >= UltraStacksConfig.limitCap()) {
            cir.setReturnValue(stack);
        }
    }
}
