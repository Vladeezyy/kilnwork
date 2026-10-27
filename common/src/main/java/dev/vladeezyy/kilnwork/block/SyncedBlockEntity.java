package dev.vladeezyy.kilnwork.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Block entity with the data the client needs written in one place, {@link #saveSynced(CompoundTag, HolderLookup.Provider)}
 */
public class SyncedBlockEntity extends BlockEntity {
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
  protected void saveSynced(CompoundTag nbt, HolderLookup.Provider registries) {}

  @Override
  @Nullable
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return shouldSyncOnUpdate() ? ClientboundBlockEntityDataPacket.create(this) : null;
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    CompoundTag nbt = new CompoundTag();
    saveSynced(nbt, registries);
    return nbt;
  }

  @Override
  protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
    super.saveAdditional(nbt, registries);
    saveSynced(nbt, registries);
  }
}
