package dev.vladeezyy.kilnwork.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Item handler slot respecting the handler's slot limit. Based on Mantle's SmartItemHandlerSlot (MIT, SlimeKnights). */
public class SmartItemHandlerSlot extends SlotItemHandler {
  public SmartItemHandlerSlot(IItemHandler handler, int index, int x, int y) {
    super(handler, index, x, y);
  }

  @Override
  public boolean mayPickup(Player player) {
    return getItem().isEmpty() || super.mayPickup(player);
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return Math.min(stack.getMaxStackSize(), getItemHandler().getSlotLimit(getSlotIndex()));
  }
}
