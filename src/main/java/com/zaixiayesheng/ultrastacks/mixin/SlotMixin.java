package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 槽位上限：把原版容器的 64 与某些槽位自己写死的 99 换成配置的堆叠上限。
 *
 * <p>两条注入各管一条路径。无参 {@code getMaxStackSize()} 是槽位报给菜单的值；带 {@link ItemStack} 参数的
 * 重载是 Forge 的插入路径（{@code safeInsert}）与点击处理真正调用的那个。有的槽位（酿造台的燃料槽、
 * 药水槽）自己重写了无参版本并直接返回 64 或 1，所以两条都要拦。
 *
 * <p>无参版本还要保证返回值不小于槽内已有数量：菜单用「上限减已有数量」算剩余空间，配置调小之后
 * 这个减法会是负数，格子里的东西会凭空变少。带参数的重载只在物品相同时才按数量抬，否则槽里放着一堆
 * 铁锭，16 个一组的鸡蛋也能塞进同样多。
 *
 * <p>Slot limits: the container default of 64 and the 99 some slots hardcode become the configured stack
 * size. The two injections cover the two paths. The no-argument {@code getMaxStackSize()} is what a slot
 * reports to the menu; the overload taking an {@link ItemStack} is what Forge's insert path
 * ({@code safeInsert}) and the click handling actually call. Some slots, such as the fuel and potion
 * slots of a brewing stand, override the no-argument version and return 64 or 1 outright, which is why
 * both are needed. The no-argument version also keeps the result at least at the count already inside,
 * because the menu computes headroom as "limit minus count" and a lowered config would otherwise make
 * that negative and quietly shrink the slot. The overload only raises by the current count when the item
 * matches, or a grid holding an ingot stack would accept as many 16-stack eggs.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$raiseVanillaLimit(CallbackInfoReturnable<Integer> cir) {
        // 原版容器的默认上限是 64，部分槽位自己写死 99，所以两个值一起映射。
        int raised = UltraStacksConfig.raiseLegacyDefault(cir.getReturnValue());
        ItemStack current = ((Slot) (Object) this).getItem();
        cir.setReturnValue(current.isEmpty() ? raised : Math.max(raised, current.getCount()));
    }

    /**
     * 带 {@link ItemStack} 参数的重载：Forge 的 {@code safeInsert} 与点击处理走的是这条路。
     * 部分槽位自己重写了无参版本，绕过了上面那条注入，所以这里也把 64 与 99 抬到配置上限。
     *
     * <p>The overload taking an {@link ItemStack}, which is the path Forge's {@code safeInsert} and the
     * click handling use. Slots that override the no-argument version bypass the injection above, so the
     * hardcoded 64 and 99 are raised here as well.
     */
    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void ultrastacks$neverBelowContents(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int raised = UltraStacksConfig.raiseLegacyDefault(cir.getReturnValue());
        ItemStack current = ((Slot) (Object) this).getItem();
        if (!current.isEmpty() && ItemStack.isSameItemSameTags(current, stack)) {
            raised = Math.max(raised, current.getCount());
        }
        cir.setReturnValue(raised);
    }
}
