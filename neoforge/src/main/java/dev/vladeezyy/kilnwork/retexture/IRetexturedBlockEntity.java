package dev.vladeezyy.kilnwork.retexture;

import dev.vladeezyy.kilnwork.KilnworkRegistries;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Block entity of a retextured block. Based on Mantle's IRetexturedBlockEntity (MIT, SlimeKnights).
 * @see RetexturedBlockEntity
 */
public interface IRetexturedBlockEntity {
  /** Current texture, air if none */
  Block getTexture();

  /** Sets the texture, calls {@link RetexturedHelper#onTextureUpdated} if it changed */
  void updateTexture(Block texture);

  /** Name of the texture, empty if none */
  default String getTextureName() {
    return RetexturedHelper.getTextureName(getTexture());
  }

  /** Implementation of {@code BlockEntity#collectImplicitComponents} */
  static void collectImplicitComponents(IRetexturedBlockEntity blockEntity, DataComponentMap.Builder builder) {
    Block texture = blockEntity.getTexture();
    if (texture != Blocks.AIR) {
      builder.set(KilnworkRegistries.TEXTURE.get(), texture);
    }
  }
}
