package dev.vladeezyy.kilnwork.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
  protected void applyImplicitComponents(DataComponentGetter components) {
    super.applyImplicitComponents(components);
    this.customName = components.get(DataComponents.CUSTOM_NAME);
  }

  @Override
  protected void collectImplicitComponents(DataComponentMap.Builder components) {
    super.collectImplicitComponents(components);
    components.set(DataComponents.CUSTOM_NAME, this.customName);
  }

  @Override
  public void removeComponentsFromTag(ValueOutput output) {
    super.removeComponentsFromTag(output);
    output.discard(TAG_CUSTOM_NAME);
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    this.customName = BlockEntity.parseCustomNameSafe(input, TAG_CUSTOM_NAME);
  }

  @Override
  protected void saveSynced(ValueOutput output) {
    super.saveSynced(output);
    output.storeNullable(TAG_CUSTOM_NAME, ComponentSerialization.CODEC, this.customName);
  }
}
