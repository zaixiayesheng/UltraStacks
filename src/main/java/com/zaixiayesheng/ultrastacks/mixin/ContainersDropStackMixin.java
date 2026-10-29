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
 * 容器被破坏时，超过阈值的堆叠整堆掉成一个掉落物，而不是散成一地小堆。
 *
 * <p>原版在 {@code dropItemStack} 里按 {@code random.nextInt(21) + 10} 循环切分，每份 10 到 30 个；
 * 一个 65536 的堆叠会变成三千多个掉落物实体，服务端要逐个生成、客户端要逐个渲染。这里对数量大于 128 的
 * 堆叠直接整摞切下来生成一个实体，初速度沿用原版的三角分布，普通堆叠保持原版散落行为。
 *
 * <p>Drops a superstack as a single item entity when a container is broken, instead of scattering
 * it into many small piles. Vanilla splits in a loop of {@code random.nextInt(21) + 10} items, ten
 * to thirty per entity, so a 65536 stack becomes more than three thousand entities for the server
 * to spawn and the client to render. Stacks above 128 are split off in one piece and spawned with
 * the same triangular velocity vanilla uses; ordinary stacks keep the vanilla scatter.
 */
@Mixin(Containers.class)
public abstract class ContainersDropStackMixin {

    /**
     * 超过这个数量的堆叠不再散落，而是整堆掉落。128 是「明显超出普通堆叠、又不会多到卡顿」的分界。
     *
     * <p>Stacks above this count drop as one entity instead of scattering. 128 sits well above an
     * ordinary stack without being large enough to cause a stutter.
     */
    private static final int SUPERSTACK_SPLIT_THRESHOLD = 128;

    /**
     * 整摞切下来生成一个掉落物，然后取消原版的切分循环。
     *
     * <p>{@code split(全部数量)} 会把传入的 {@code stack} 清空，所以原版循环拿到的已经是空堆叠，不会重复掉落。
     *
     * <p>Splits off the whole stack into one item entity and cancels vanilla's splitting loop.
     * {@code split} with the full count empties the incoming stack, so the vanilla loop finds nothing
     * left to drop.
     */
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
