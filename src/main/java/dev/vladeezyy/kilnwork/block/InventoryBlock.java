package dev.vladeezyy.kilnwork.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

import javax.annotation.Nullable;

/**
 * Block with an inventory and a menu: the block entity is a {@link MenuProvider}; a block entity that is a
 * {@link net.minecraft.world.Container} drops its items when the block is removed (vanilla, since 1.21.5). Based on Mantle's InventoryBlock (MIT, SlimeKnights).
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
  protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
    Containers.updateNeighboursAfterDestroy(state, level, pos);
  }

  @Override
  protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
    super.triggerEvent(state, level, pos, id, param);
    BlockEntity be = level.getBlockEntity(pos);
    return be != null && be.triggerEvent(id, param);
  }
}
