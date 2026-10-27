package dev.vladeezyy.kilnwork.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;

/** Small helpers for block entities */
public final class BlockEntityHelper {
  private BlockEntityHelper() {}

  /** True if the position is loaded, never loads a chunk */
  public static boolean isBlockLoaded(@Nullable BlockGetter world, BlockPos pos) {
    if (world == null) {
      return false;
    }
    if (world instanceof LevelReader reader) {
      return reader.hasChunkAt(pos);
    }
    return true;
  }

  /** Ticker running on the server only, for the expected block entity type */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> serverTicker(Level level, BlockEntityType<A> type, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
    return !level.isClientSide && type == expected ? (BlockEntityTicker<A>) ticker : null;
  }
}
