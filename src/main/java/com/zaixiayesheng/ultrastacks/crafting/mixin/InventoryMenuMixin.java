package com.zaixiayesheng.ultrastacks.crafting.mixin;

import com.zaixiayesheng.ultrastacks.crafting.BulkCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 接管玩家背包 2x2 合成网格产物槽的 Shift 点击，与工作台共用同一套批量合成逻辑。
 *
 * <p>注入点同样是 {@code quickMoveStack} 的头部，产物槽也是索引 0，只是背包格从 {@code 9} 到
 * {@code 46}：这个菜单里结果槽、网格与护甲槽排在最前面，{@code 9} 之后才是玩家背包，一直到快捷栏末
 * 尾，{@code 45} 的副手槽不在范围内。
 *
 * <p>Takes over shift-clicks on the result slot of the 2x2 grid in the player's own inventory menu,
 * sharing the bulk-crafting logic with the crafting table.
 *
 * <p>The injection point is the head of {@code quickMoveStack} again and the result slot is index 0
 * again; only the player inventory range differs, {@code 9} to {@code 46}. In this menu the result
 * slot, the crafting grid and the armour slots come first, so the player's inventory starts at
 * {@code 9} and runs through the end of the hotbar, while the offhand slot at {@code 45} is left out.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$bulkCraft(Player player, int slotId, CallbackInfoReturnable<ItemStack> cir) {
        if (slotId != 0) {
            return;
        }
        AbstractCraftingMenu menu = (AbstractCraftingMenu) (Object) this;
        if (BulkCrafting.tryCraftAll(menu, player, 9, 45)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
