package dev.vladeezyy.kilnwork.retexture;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Block taking its texture from another block, see {@link IRetexturedBlockEntity}. Based on Mantle's RetexturedBlock
 * (MIT, SlimeKnights).
 */
public abstract class RetexturedBlock extends Block implements EntityBlock {
  public RetexturedBlock(Properties properties) {
    super(properties);
  }

  @Override
  public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
    return getPickBlock(level, pos, state);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    RetexturedHelper.addTooltip(stack, tooltip, flag);
  }

  /** Stack of the block with the texture of its block entity */
  public static ItemStack getPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
    ItemStack stack = new ItemStack(state.getBlock());
    if (level.getBlockEntity(pos) instanceof IRetexturedBlockEntity be) {
      RetexturedHelper.setTexture(stack, be.getTexture());
    }
    return stack;
  }
}
