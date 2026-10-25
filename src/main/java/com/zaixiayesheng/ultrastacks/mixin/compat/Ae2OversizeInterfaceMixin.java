package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ExtendedAE 的超大接口用自己的 {@code OversizeConfigInv} 算容量，父类 {@code GenericStackInv} 上的那处注入到不了这里，所以单独补一层。
 *
 * <p>换算是 {@link UltraStacksConfig#ae2InterfaceCapacity(long)}：先按上限 / 16 抬高，再夹到不超过配置上限本身，避免单个槽位比玩家能拿的一摞还多。
 *
 * <p>ExtendedAE's oversized interface computes its capacity in its own {@code OversizeConfigInv}, which
 * the hook on the parent {@code GenericStackInv} cannot reach, so a second hook is added here.
 *
 * <p>The value goes through {@link UltraStacksConfig#ae2InterfaceCapacity(long)}: it is first raised to
 * a sixteenth of the limit and then clamped to the limit itself, so one slot never holds more than the
 * player can carry in a single stack.
 */
@Mixin(targets = "com.glodblock.github.extendedae.common.inventory.OversizeConfigInv", remap = false)
public abstract class Ae2OversizeInterfaceMixin {

    @ModifyReturnValue(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), require = 0)
    private long ultrastacks$raiseOversizeCapacity(long original) {
        return UltraStacksConfig.ae2InterfaceCapacity(original);
    }
}
