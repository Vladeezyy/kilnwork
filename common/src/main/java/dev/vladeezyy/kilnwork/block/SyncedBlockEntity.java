package dev.vladeezyy.kilnwork.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Block entity with the data the client needs written in one place, {@link #saveSynced(ValueOutput)}
 */
public class SyncedBlockEntity extends BlockEntity {
  private static final Logger LOG = LoggerFactory.getLogger("Kilnwork");

  public SyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  /** Marks the chunk as changed without the comparator and block state updates of {@link #setChanged()} */
  public void setChangedFast() {
    if (level != null) {
      level.blockEntityChanged(worldPosition);
    }
  }

  /** If true, the synced data is sent to clients on block updates */
  protected boolean shouldSyncOnUpdate() {
    return false;
  }

  /** Writes data saved to disk and sent to the client */
  protected void saveSynced(ValueOutput output) {}

  @Override
  @Nullable
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return shouldSyncOnUpdate() ? ClientboundBlockEntityDataPacket.create(this) : null;
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    try (ProblemReporter.ScopedCollector problems = new ProblemReporter.ScopedCollector(problemPath(), LOG)) {
      TagValueOutput output = TagValueOutput.createWithContext(problems, registries);
      saveSynced(output);
      return output.buildResult();
    }
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    saveSynced(output);
  }
}
