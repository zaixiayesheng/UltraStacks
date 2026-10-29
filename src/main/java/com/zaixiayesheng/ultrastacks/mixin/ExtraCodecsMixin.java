package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 把 {@code ExtraCodecs.intRange(1, 99)} 的上界从 99 放宽到硬上限 {@code ABSOLUTE_MAX_STACK_SIZE}。
 *
 * <p>原版带 99 这个上界的只有两处：{@code DataComponents.MAX_STACK_SIZE}（{@code max_stack_size} 组件）
 * 和 {@code ItemStack} 序列化里的 {@code count} 字段。不改上界，堆叠数量在读取阶段就会被 codec 判成越界，
 * 存档直接读不回来。
 *
 * <p>新上界用编译期常量而不是配置值：这些 codec 是静态字段，类初始化时就建好，早于配置文件加载，那时读
 * 不到配置。只替换上界等于 99 的调用，属性等级、生成范围等其它 {@code intRange} 用法不受影响。
 *
 * <p>Widens the upper bound of {@code ExtraCodecs.intRange(1, 99)} from 99 to the hard limit
 * {@code ABSOLUTE_MAX_STACK_SIZE}.
 *
 * <p>Exactly two places in vanilla use that 99 bound: {@code DataComponents.MAX_STACK_SIZE} (the
 * {@code max_stack_size} component) and the {@code count} field of {@code ItemStack}'s serialisation.
 * Without the wider bound a large count is rejected by the codec on read and the save cannot be loaded.
 *
 * <p>The new bound is a compile-time constant rather than a config value because those codecs are static
 * fields built during class initialisation, before the config file is loaded, when no config value is
 * available. Only calls whose upper bound is 99 are replaced; other {@code intRange} uses such as
 * attribute levels and spawn ranges are untouched.
 */
@Mixin(ExtraCodecs.class)
public abstract class ExtraCodecsMixin {
    @ModifyVariable(method = "intRange", at = @At("HEAD"), argsOnly = true, index = 1)
    private static int ultrastacks$raiseUpperBound(int bound) {
        return UltraStacksConfig.raiseCodecUpperBound(bound);
    }
}
