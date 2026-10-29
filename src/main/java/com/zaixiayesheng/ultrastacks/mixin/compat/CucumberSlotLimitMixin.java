package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 神秘农业及其附属机器走 Cucumber 的槽位处理器，处理器把每格上限默认成 64。
 *
 * <p>物品本身能叠到 65536，机器的输入格与输出格却只收 64 个，多出来的部分根本放不进去。这里把 64 与 99
 * 两种原版尺寸的默认值换成配置上限，显式设过别的上限的槽位保持原值；兼容开关关闭时原样返回。
 *
 * <p>Mystical Agriculture and its addons use Cucumber's slot handler, which defaults every slot to 64.
 *
 * <p>The items themselves stack to 65536, but a machine's input and output slots only take 64 of them, so the
 * rest cannot be inserted at all. The two vanilla-sized defaults, 64 and 99, are replaced with the configured
 * limit, slots with an explicitly different limit keep it, and the value is returned unchanged while the
 * compat switch is off.
 */
@Mixin(targets = "com.blakebr0.cucumber.inventory.CItemStacksHandler", remap = false)
public abstract class CucumberSlotLimitMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.mysticalSlotLimit(cir.getReturnValue()));
    }
}
