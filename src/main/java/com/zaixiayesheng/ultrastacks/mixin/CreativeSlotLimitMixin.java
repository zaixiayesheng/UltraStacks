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