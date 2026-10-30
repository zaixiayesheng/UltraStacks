package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 抽屉一次能倒出多少。
 *
 * <p>按住 Shift 点抽屉，FS 会先问一句「这格最多能拿多少」，然后照着这个数往背包里塞。它这答案跟容量
 * 挂钩——上限还是 64 的时候，最多一组，正常；抬到 65536 之后，一个存了两万多的抽屉一按就全进背包了，
 * 格子瞬间塞满，剩下的还掉地上。所以这里把单次取出压成一组。管子、漏斗、AE2 走的是物品接口，不经过
 * 这个方法，不受影响。</p>
 *
 * <p>1.20.1 没有数据组件，旧存档也不会留「上限 64」的痕迹，这棵树就只管取出量，不用像 1.21.1 那样
 * 还得洗模板。</p>
 *
 * <p>Shift-clicking a drawer asks FS "how much can this slot give", and it answers with the slot
 * capacity — fine at 64, catastrophic at 65536 where one click empties twenty thousand dirt into
 * your backpack. Capped to one stack here. Pipes, hoppers and AE2 use the item handler instead, so
 * they are unaffected. No data components in 1.20.1, so no legacy templates to clean.</p>
 */
@Mixin(targets = "com.buuz135.functionalstorage.inventory.BigInventoryHandler", remap = false)
public abstract class FunctionalStorageDrawerMixin {

    /**
     * 把这次请求的取出量夹到一组；请求本来就比一组小的原样放行，别把「只拿一个」变成「拿一组」。
     *
     * <p>Clamps the requested amount to one stack, leaving smaller requests alone so that taking a
     * single item stays a single item.</p>
     */
    @ModifyVariable(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"),
            argsOnly = true, index = 2, require = 0)
    private int ultrastacks$capExtract(int amount) {
        int cap = UltraStacksConfig.maxStackSize();
        return amount > cap ? cap : amount;
    }
}
