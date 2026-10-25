package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.ItemMaxStackAccess;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 物品上限的源头：{@link Item} 的私有字段 {@code maxStackSize}，{@code getMaxStackSize()} 原样返回它。
 *
 * <p>普通物品都从这个方法拿上限，所以一次缩放就能覆盖它们：本来能堆的按 {@code scaledMaxForItem} 放大，
 * 原本只能堆 1 个的（工具、盔甲）保持 1，药水、附魔书、桶这些要按堆叠内容单独判断的，交给
 * {@code ItemStack} 那侧的 {@code limitFor}。
 *
 * <p>本 mixin 同时通过 {@link ItemMaxStackAccess} 把这个字段暴露出去，{@code limitFor} 才有办法区分
 * 「原版上限」和「当前生效上限」。
 *
 * <p>Where the item limit comes from: the private {@code maxStackSize} field of {@link Item}, which
 * {@code getMaxStackSize()} returns unchanged. Ordinary items read their limit from this method, so
 * scaling it once covers them: items that already stack are raised by {@code scaledMaxForItem}, items
 * that only stack to one (tools, armour) stay at one, and cases that have to look at the stack itself,
 * namely potions, enchanted books and buckets, are left to {@code limitFor} on the {@code ItemStack}
 * side. The mixin also exposes the field through {@link ItemMaxStackAccess}, which is what lets
 * {@code limitFor} tell the vanilla limit apart from the one in effect.
 */
@Mixin(Item.class)
public abstract class ItemMixin implements ItemMaxStackAccess {
    @Shadow
    @Final
    private int maxStackSize;

    @Override
    public int ultrastacks$vanillaMaxStackSize() {
        return this.maxStackSize;
    }

    @Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$raiseMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.scaledMaxForItem(this.maxStackSize));
    }
}
