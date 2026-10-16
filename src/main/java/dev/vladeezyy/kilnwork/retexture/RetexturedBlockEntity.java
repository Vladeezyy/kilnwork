package dev.vladeezyy.kilnwork.retexture;

import dev.vladeezyy.kilnwork.KilnworkRegistries;
import dev.vladeezyy.kilnwork.block.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

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
  protected void applyImplicitComponents(DataComponentInput input) {
    super.applyImplicitComponents(input);
    Block block = input.get(KilnworkRegistries.TEXTURE);
    this.texture = block != null ? block : Blocks.AIR;
  }

  @Override
  protected void collectImplicitComponents(DataComponentMap.Builder components) {
    super.collectImplicitComponents(components);
    IRetexturedBlockEntity.collectImplicitComponents(this, components);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void removeComponentsFromTag(CompoundTag tag) {
    super.removeComponentsFromTag(tag);
    tag.remove(RetexturedHelper.TAG_TEXTURE);
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(CompoundTag tags, HolderLookup.Provider registries) {
    super.saveSynced(tags, registries);
    if (texture != Blocks.AIR) {
      tags.putString(RetexturedHelper.TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  protected void loadAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.loadAdditional(tags, registries);
    String name = readTextureName(tags);
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
  public static String readTextureName(CompoundTag tags) {
    if (tags.contains(RetexturedHelper.TAG_TEXTURE, Tag.TAG_STRING)) {
      return tags.getString(RetexturedHelper.TAG_TEXTURE);
    }
    for (String key : new String[] {"NeoForgeData", "ForgeData"}) {
      if (tags.contains(key, Tag.TAG_COMPOUND)) {
        CompoundTag data = tags.getCompound(key);
        if (data.contains(RetexturedHelper.TAG_TEXTURE, Tag.TAG_STRING)) {
          return data.getString(RetexturedHelper.TAG_TEXTURE);
        }
      }
    }
    return null;
  }
}
