package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 {@code getMaxStackSize()} 里直接算出堆叠上限，并保证它永远不小于堆叠自带的数量。
 *
 * <p>上限不走「注册时写 {@code MAX_STACK_SIZE} 组件」那条路：物品组件在注册阶段还没绑定完，那时读这个组件
 * 拿不到可靠值；改成在读取方法上返回配置值，既绕开这个时序问题，也顺带覆盖了游戏运行中改配置的情形。
 *
 * <p>上限还必须不小于堆叠自带的数量。原版菜单把空余空间算成 {@code 上限 - 数量}，上限一旦比数量小，这个差
 * 就是负数：{@code grow(负数)} 让槽位变小，{@code shrink(负数)} 让手里的堆叠变大，玩家看到的就是「拆开再
 * 放回去，一半消失了 / 多出一个」。所以先按配置算上限，再抬到不小于 {@code getCount()}。
 *
 * <p>Computes the stack limit directly in {@code getMaxStackSize()} and never reports less than the
 * number of items the stack already carries.
 *
 * <p>The limit is not written as a {@code MAX_STACK_SIZE} component during registration: item components
 * are not fully bound at that point and reading one there does not give a dependable value. Returning the
 * configured limit from the getter sidesteps that ordering problem and also covers changing the config
 * while the game is running.
 *
 * <p>The limit must also never be smaller than the count the stack carries. Vanilla menus compute the free
 * headroom as {@code limit - count}, so once the limit drops below the count that difference goes negative:
 * {@code grow(negative)} shrinks the slot while {@code shrink(negative)} grows the stack in hand, which is
 * exactly the "split it, put both halves back, and half of it vanishes / one extra appears" symptom. The
 * limit is therefore taken from the config first and then raised to at least {@code getCount()}.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMaxStackMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$neverBelowCount(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        int max = UltraStacksConfig.limitFor(self, cir.getReturnValue());
        if (self.getCount() > max) {
            max = self.getCount();
        }
        if (max != cir.getReturnValue()) {
            cir.setReturnValue(max);
        }
    }
}
