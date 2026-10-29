package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 创造模式往背包里放、往地上丢的数量上限。
 *
 * <p>{@code ServerGamePacketListenerImpl.handleSetCreativeModeSlot} 里写死了
 * {@code itemstack.getCount() <= 64} 的判断，超过 64 的创造物品包会被服务端直接扔掉。客户端本地显示
 * 32768，服务端却当成非法包忽略，表现就是「拿出来的东西一开箱子就没了、丢到地上闪一下就消失」。
 * 把这个常量换成配置的堆叠上限，创造拿出来的大堆叠才会真正落到服务端。
 *
 * <p>The creative-mode count limit when filling the inventory or dropping an item. The check
 * {@code itemstack.getCount() <= 64} is hardcoded in
 * {@code ServerGamePacketListenerImpl.handleSetCreativeModeSlot}, and a creative packet above 64 is
 * dropped by the server: the client shows 32768 locally while the server ignores it as invalid, so the
 * item vanishes as soon as a container is opened or as soon as it hits the ground. Replacing the
 * constant with the configured stack size lets those stacks arrive.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeSlotLimitMixin {
    @ModifyConstant(
            method = "handleSetCreativeModeSlot",
            constant = @Constant(intValue = 64),
            require = 0
    )
    private int ultrastacks$raiseCreativeSlotLimit(int original) {
        return UltraStacksConfig.maxStackSize();
    }
}