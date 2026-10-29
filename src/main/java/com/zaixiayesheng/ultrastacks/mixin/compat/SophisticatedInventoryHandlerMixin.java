package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让精妙储存与精妙背包的槽位放得下整摞配置大小的物品。
 *
 * <p>库存把存储槽报成 64、升级槽报成 99，玩家或漏斗往里推整摞物品时就会被拆开、拒收或悄悄截断。这里改的是
 * 报出的上限而不是物品自身，所以既有的搬运动作不受影响。两处都要改：{@code getInternalSlotLimit} 决定每格
 * 能放多少，{@code getBaseCapacity} 决定 transfer 层一次能搬多少。
 *
 * <p>Lets the slots of Sophisticated Storage and Sophisticated Backpacks hold whole stacks of the configured
 * size.
 *
 * <p>The inventory reports 64 for storage slots and 99 for upgrade slots, so a larger stack is split, refused
 * or silently truncated when a player or a hopper pushes it in. Only the reported limit is changed, not the
 * item itself, so existing transfer paths keep working. Both hooks are needed: {@code getInternalSlotLimit}
 * decides how much one slot holds, while {@code getBaseCapacity} decides how much the transfer layer moves at
 * once.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler", remap = false)
public abstract class SophisticatedInventoryHandlerMixin {

    @Inject(method = "getInternalSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }

    @Inject(method = "getBaseCapacity(Lnet/neoforged/neoforge/transfer/item/ItemResource;)I",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseCapacity(ItemResource resource, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.raiseSophisticatedLimit(cir.getReturnValueI()));
    }
}
