package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 容器被破坏时，大堆叠整堆掉出来。
 *
 * <p>原版 {@code dropItemStack} 用 {@code stack.split(random.nextInt(21) + 10)} 循环把内容物拆成
 * 10 到 30 个一份，一整堆 65536 会散成三千多个物品实体，服务端卡顿、客户端也会被刷屏。
 * 数量超过 128 的堆叠因此只生成一个实体，普通堆叠仍旧按原版散落。
 *
 * <p>Large stacks drop in one piece when a container breaks. Vanilla {@code dropItemStack} loops over
 * {@code stack.split(random.nextInt(21) + 10)}, splitting the contents into pieces of 10 to 30, so a
 * single stack of 65536 becomes over three thousand item entities and stalls the server while flooding
 * clients. Stacks above 128 therefore spawn one entity, while ordinary stacks keep the vanilla scatter.
 */
@Mixin(Containers.class)
public abstract class ContainersDropStackMixin {

    private static final int SUPERSTACK_SPLIT_THRESHOLD = 128;

    @Inject(method = "dropItemStack", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$dropAsSingleEntity(Level level, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (stack.getCount() <= SUPERSTACK_SPLIT_THRESHOLD) {
            return;
        }
        ItemStack dropped = stack.split(stack.getCount());
        ItemEntity entity = new ItemEntity(level, x, y, z, dropped);
        entity.setDeltaMovement(
                level.random.triangle(0.0, 0.11485000171139836),
                level.random.triangle(0.2, 0.11485000171139836),
                level.random.triangle(0.0, 0.11485000171139836));
        level.addFreshEntity(entity);
        ci.cancel();
    }
}