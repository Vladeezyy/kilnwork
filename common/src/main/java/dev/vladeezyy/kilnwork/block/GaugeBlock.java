package dev.vladeezyy.kilnwork.block;

import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import dev.vladeezyy.kilnwork.fluid.FluidText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;

import javax.annotation.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

/**
 * Small block placed on the side of a tank, shows the fluid inside. Add it to
 * {@link dev.vladeezyy.kilnwork.tag.KilnworkTags.Blocks#ATTACHED_GAUGES} for the tooltip while looking at it.
 */
public class GaugeBlock extends Block {
  private static final VoxelShape[] BOUNDS = {
    box( 4, 15,  4, 12, 16, 12), // down
    box( 4,  0,  4, 12,  1, 12), // up
    box( 4,  4, 15, 12, 12, 16), // north
    box( 4,  4,  0, 12, 12,  1), // south
    box(15,  4,  4, 16, 12, 12), // west
    box( 0,  4,  4,  1, 12, 12)  // east
  };

  public GaugeBlock(Properties properties) {
    super(properties);
    this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    // show the contents of the tank behind in the action bar
    if (!level.isClientSide()) {
      Direction side = state.getValue(FACING);
      IFluidHandler handler = KilnworkPlatform.get().fluidHandler(level, pos.relative(side.getOpposite()), side);
      if (handler != null && handler.getTanks() > 0) {
        FluidStack fluid = handler.getFluidInTank(0);
        int capacity = handler.getTankCapacity(0);
        player.displayClientMessage(fluid.isEmpty() ? FluidText.capacity(capacity) : FluidText.contents(fluid, capacity), true);
      }
    }
    return InteractionResult.SUCCESS;
  }

  @Override
  protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return BOUNDS[state.getValue(FACING).get3DDataValue()];
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    if (level instanceof Level world) {
      Direction direction = state.getValue(FACING);
      return KilnworkPlatform.get().fluidHandler(world, pos.relative(direction.getOpposite()), direction) != null;
    }
    // no capabilities during worldgen
    return true;
  }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    BlockState state = this.defaultBlockState();
    for (Direction direction : context.getNearestLookingDirections()) {
      state = state.setValue(FACING, direction.getOpposite());
      if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
        return state;
      }
    }
    return null;
  }

  @Override
  protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
    return facing.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
  }

  @Override
  protected BlockState rotate(BlockState state, Rotation rotation) {
    return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
  }

  @Override
  protected BlockState mirror(BlockState state, Mirror mirror) {
    return state.rotate(mirror.getRotation(state.getValue(FACING)));
  }
}
