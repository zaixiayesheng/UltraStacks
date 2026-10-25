package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 放宽 {@code ExtraCodecs.intRange} 的上界：只有上界正好是 99 时才换成
 * {@code UltraStacks.ABSOLUTE_MAX_STACK_SIZE}（1073741823）。
 *
 * <p>用到这个范围的两处是 {@code DataComponents.MAX_STACK_SIZE} 的持久化 codec 与
 * {@code ItemStack.CODEC} 的 {@code count} 字段，99 会让大堆叠在反序列化阶段（存档、网络包）被判为非法值。
 * 新上界用编译期常量而不是配置值，因为 codec 在配置加载之前就已经建好了，那时读不到 config。
 *
 * <p>Widens the upper bound of {@code ExtraCodecs.intRange}: only a bound that is exactly 99 is
 * replaced with {@code UltraStacks.ABSOLUTE_MAX_STACK_SIZE} (1073741823). The range is used by the
 * persistent codec of {@code DataComponents.MAX_STACK_SIZE} and by the {@code count} field of
 * {@code ItemStack.CODEC}, where 99 would reject large stacks while deserializing saves or network
 * packets. The new bound is a compile-time constant rather than a configured value because the
 * codec is built before the config is loaded.
 */
@Mixin(ExtraCodecs.class)
public abstract class ExtraCodecsMixin {
    @ModifyVariable(method = "intRange", at = @At("HEAD"), argsOnly = true, index = 1)
    private static int ultrastacks$raiseUpperBound(int bound) {
        return UltraStacksConfig.raiseCodecUpperBound(bound);
    }
}
