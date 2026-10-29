package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 更多熔炉的 {@code setItem} 把自己容器报的上限当夹取上限。
 *
 * <p>这个上限不能直接抬：{@code load} 里用 {@code NonNullList.withSize(getMaxStackSize(), …)} 按它分配
 * 列表长度，抬高之后读档会分配一个巨大的列表。所以只把 {@code setItem} 里那一次夹取换成配置值。
 *
 * <p>注入写的是正式环境的名字：{@code m_6836_} 即 {@code setItem}，{@code m_6893_} 即
 * {@code getMaxStackSize}，这两个重写在正式包里带 SRG 名。名称匹配不上时靠 {@code require = 0}
 * 静默跳过，开发环境不会因此报错。
 *
 * <p>The {@code setItem} of an Iron Furnaces container clamps to the limit that same container reports.
 * That limit cannot simply be raised: {@code load} sizes its list with
 * {@code NonNullList.withSize(getMaxStackSize(), …)}, so a larger value would allocate a huge list while
 * reading the save. Only the single clamp inside {@code setItem} is replaced with the configured value.
 * The injected names are the production ones: {@code m_6836_} is {@code setItem} and {@code m_6893_} is
 * {@code getMaxStackSize}, both carrying SRG names in the production jar, and {@code require = 0} makes a
 * name mismatch a silent skip rather than an error in the development environment.
 */
@Mixin(targets = "ironfurnaces.tileentity.TileEntityInventory", remap = false)
public abstract class IronFurnacesInventoryMixin {

    @Redirect(method = "m_6836_(ILnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lironfurnaces/tileentity/TileEntityInventory;m_6893_()I"),
            remap = false, require = 0)
    private int ultrastacks$raiseSetItemCap() {
        return UltraStacksConfig.ironFurnaceLimit(64);
    }
}
