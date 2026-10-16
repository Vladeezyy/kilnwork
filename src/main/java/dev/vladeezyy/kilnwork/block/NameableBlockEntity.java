package dev.vladeezyy.kilnwork.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Block entity with a menu and a name that can be set with a renamed item, without vanilla's container interface.
 * Based on Mantle's NameableBlockEntity (MIT, SlimeKnights).
 */
public abstract class NameableBlockEntity extends SyncedBlockEntity implements MenuProvider, Nameable {
  private static final String TAG_CUSTOM_NAME = "CustomName";

  private final Component defaultName;
  @Nullable
  private Component customName;

  public NameableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component defaultName) {
    super(type, pos, state);
    this.defaultName = defaultName;
  }

  public Component getDefaultName() {
    return defaultName;
  }

  @Nullable
  @Override
  public Component getCustomName() {
    return customName;
  }

  public void setCustomName(@Nullable Component name) {
    this.customName = name;
  }

  @Override
  public Component getName() {
    return customName != null ? customName : defaultName;
  }

  @Override
  public Component getDisplayName() {
    return getName();
  }

  @Override
  protected void applyImplicitComponents(DataComponentInput input) {
    super.applyImplicitComponents(input);
    this.customName = input.get(DataComponents.CUSTOM_NAME);
  }

  @Override
  protected void collectImplicitComponents(DataComponentMap.Builder components) {
    super.collectImplicitComponents(components);
    components.set(DataComponents.CUSTOM_NAME, this.customName);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void removeComponentsFromTag(CompoundTag tag) {
    super.removeComponentsFromTag(tag);
    tag.remove(TAG_CUSTOM_NAME);
  }

  @Override
  protected void loadAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.loadAdditional(tags, registries);
    if (tags.contains(TAG_CUSTOM_NAME, Tag.TAG_STRING)) {
      this.customName = BlockEntity.parseCustomNameSafe(tags.getString(TAG_CUSTOM_NAME), registries);
    }
  }

  @Override
  protected void saveSynced(CompoundTag tags, HolderLookup.Provider registries) {
    super.saveSynced(tags, registries);
    if (this.customName != null) {
      tags.putString(TAG_CUSTOM_NAME, Component.Serializer.toJson(this.customName, registries));
    }
  }
}
