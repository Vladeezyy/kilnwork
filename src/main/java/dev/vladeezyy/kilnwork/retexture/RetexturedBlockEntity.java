package dev.vladeezyy.kilnwork.retexture;

import dev.vladeezyy.kilnwork.KilnworkRegistries;
import dev.vladeezyy.kilnwork.block.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * Block entity holding only a texture. Based on Mantle's DefaultRetexturedBlockEntity (MIT, SlimeKnights).
 */
public class RetexturedBlockEntity extends SyncedBlockEntity implements IRetexturedBlockEntity {
  private Block texture = Blocks.AIR;

  public RetexturedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Override
  public Block getTexture() {
    return texture;
  }

  @Override
  public ModelData getModelData() {
    return RetexturedHelper.getModelData(texture);
  }

  @Override
  public void updateTexture(Block texture) {
    Block old = this.texture;
    this.texture = texture;
    if (old != texture) {
      setChangedFast();
      RetexturedHelper.onTextureUpdated(this);
    }
  }

  @Override
  protected void applyImplicitComponents(DataComponentGetter components) {
    super.applyImplicitComponents(components);
    Block block = components.get(KilnworkRegistries.TEXTURE);
    this.texture = block != null ? block : Blocks.AIR;
  }

  @Override
  protected void collectImplicitComponents(DataComponentMap.Builder components) {
    super.collectImplicitComponents(components);
    IRetexturedBlockEntity.collectImplicitComponents(this, components);
  }

  @Override
  public void removeComponentsFromTag(ValueOutput output) {
    super.removeComponentsFromTag(output);
    output.discard(RetexturedHelper.TAG_TEXTURE);
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(ValueOutput output) {
    super.saveSynced(output);
    if (texture != Blocks.AIR) {
      output.putString(RetexturedHelper.TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    String name = readTextureName(input);
    if (name != null) {
      texture = RetexturedHelper.getBlock(name);
      RetexturedHelper.onTextureUpdated(this);
    }
  }

  /**
   * Texture name in block entity data, null if none. Older Mantle versions kept it in the Forge persistent data
   * ({@code ForgeData}), read that too for 1.20 worlds.
   */
  @javax.annotation.Nullable
  public static String readTextureName(ValueInput input) {
    var name = input.getString(RetexturedHelper.TAG_TEXTURE);
    if (name.isPresent()) {
      return name.get();
    }
    for (String key : new String[] {"NeoForgeData", "ForgeData"}) {
      var data = input.read(key, CompoundTag.CODEC);
      if (data.isPresent() && data.get().getString(RetexturedHelper.TAG_TEXTURE).isPresent()) {
        return data.get().getStringOr(RetexturedHelper.TAG_TEXTURE, "");
      }
    }
    return null;
  }
}
