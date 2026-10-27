package dev.vladeezyy.kilnwork.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Block with an inventory and a menu: the block entity is a {@link MenuProvider} and exposes
 * {@link Capabilities.ItemHandler#BLOCK}. Drops the items when removed. Based on Mantle's InventoryBlock (MIT, SlimeKnights).
 */
public abstract class InventoryBlock extends Block implements EntityBlock {
  protected InventoryBlock(Properties properties) {
    super(properties);
  }

  /**
   * Opens the menu of the block entity
   * @return {@link InteractionResult#CONSUME} on the server, {@link InteractionResult#SUCCESS} on the client
   */
  protected InteractionResult openGui(BlockState state, Level level, BlockPos pos, Player player) {
    if (!level.isClientSide()) {
      MenuProvider menu = getMenuProvider(state, level, pos);
      if (menu != null) {
        player.openMenu(menu, pos);
      }
      return InteractionResult.CONSUME;
    }
    return InteractionResult.SUCCESS;
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    return openGui(state, level, pos, player);
  }

  @Override
  @Nullable
  protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
    return level.getBlockEntity(pos) instanceof MenuProvider menu ? menu : null;
  }

  @Override
  protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
    if (!state.is(newState.getBlock())) {
      IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
      if (inventory != null) {
        dropInventoryItems(state, level, pos, inventory);
        level.updateNeighbourForOutputSignal(pos, this);
      }
    }
    super.onRemove(state, level, pos, newState, isMoving);
  }

  /** Drops the items when the block is replaced */
  protected void dropInventoryItems(BlockState state, Level level, BlockPos pos, IItemHandler inventory) {
    dropInventoryItems(level, pos, inventory);
  }

  /** Drops every item of the inventory at the position */
  public static void dropInventoryItems(Level level, BlockPos pos, IItemHandler inventory) {
    for (int i = 0; i < inventory.getSlots(); i++) {
      Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
    }
  }

  @Override
  protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
    super.triggerEvent(state, level, pos, id, param);
    BlockEntity be = level.getBlockEntity(pos);
    return be != null && be.triggerEvent(id, param);
  }
}
