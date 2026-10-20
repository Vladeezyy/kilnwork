package dev.vladeezyy.kilnwork.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Slot of a block entity inventory asking the container what fits ({@link Container#canPlaceItem}, its stack limit).
 * Replaces Mantle's SmartItemHandlerSlot (MIT, SlimeKnights): the inventories are plain containers since NeoForge
 * moved item handlers to the transfer API.
 */
public class ContainerSlot extends Slot {
  public ContainerSlot(Container container, int index, int x, int y) {
    super(container, index, x, y);
  }

  @Override
  public boolean mayPlace(ItemStack stack) {
    return container.canPlaceItem(getContainerSlot(), stack);
  }

  @Override
  public boolean mayPickup(Player player) {
    return getItem().isEmpty() || super.mayPickup(player);
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return Math.min(stack.getMaxStackSize(), container.getMaxStackSize(stack));
  }
}
