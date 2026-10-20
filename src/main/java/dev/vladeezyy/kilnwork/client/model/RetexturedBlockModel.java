package dev.vladeezyy.kilnwork.client.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.vladeezyy.kilnwork.retexture.RetexturedHelper;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Block model of a retextured block: the parts of the wrapped model with the quads marked by
 * {@link RetexturedModelLoader} moved onto the sprite of the block in {@link RetexturedHelper#BLOCK_PROPERTY}.
 * Retextured parts are cached; subclasses can filter quads by more model data (see {@link #extraKey}).
 */
public class RetexturedBlockModel extends DelegateBlockStateModel {
  private record Key(BlockStateModelPart part, Block texture, int extra) {}

  /** Chunk builder threads ask for parts at the same time */
  private final Cache<Key, BlockStateModelPart> parts = CacheBuilder.newBuilder().maximumSize(2048).concurrencyLevel(4).build();

  public RetexturedBlockModel(BlockStateModel delegate) {
    super(delegate);
  }

  /** More model data the quads depend on, see {@link #keep}; 0 by default */
  protected int extraKey(ModelData data) {
    return 0;
  }

  /** True if the parts need transforming even without a texture (for example to filter quads) */
  protected boolean alwaysTransform() {
    return false;
  }

  /** True if the quad is drawn with the given extra key */
  protected boolean keep(BakedQuad quad, int extra) {
    return true;
  }

  @Override
  public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> out) {
    ModelData data = level.getModelData(pos);
    Block texture = data.get(RetexturedHelper.BLOCK_PROPERTY);
    if (texture == null) {
      texture = Blocks.AIR;
    }
    int extra = extraKey(data);
    if (texture == Blocks.AIR && !alwaysTransform()) {
      delegate.collectParts(level, pos, state, random, out);
      return;
    }
    List<BlockStateModelPart> original = new ArrayList<>();
    delegate.collectParts(level, pos, state, random, original);
    for (BlockStateModelPart part : original) {
      out.add(transformed(part, state, texture, extra));
    }
  }

  /** The part retextured and filtered, cached */
  protected BlockStateModelPart transformed(BlockStateModelPart part, BlockState state, Block texture, int extra) {
    try {
      return parts.get(new Key(part, texture, extra), () -> {
        TextureAtlasSprite sprite = texture == Blocks.AIR ? null : RetexturedQuads.spriteOf(texture);
        QuadCollection quads = RetexturedQuads.transform(quadsOf(part), quad -> keep(quad, extra),
          quad -> sprite != null && RetexturedQuads.isRetextured(quad) ? RetexturedQuads.retexture(quad, sprite) : quad);
              // the layer of each quad is in its material since 26.1
        return new SimpleModelWrapper(quads, part.useAmbientOcclusion(), sprite != null ? new Material.Baked(sprite, false) : part.particleMaterial());
      });
    } catch (ExecutionException e) {
      return part;
    }
  }

  private static QuadCollection quadsOf(BlockStateModelPart part) {
    if (part instanceof SimpleModelWrapper simple) {
      return simple.quads();
    }
    QuadCollection.Builder builder = new QuadCollection.Builder();
    part.getQuads(null).forEach(builder::addUnculledFace);
    for (Direction side : Direction.values()) {
      for (BakedQuad quad : part.getQuads(side)) {
        builder.addCulledFace(side, quad);
      }
    }
    return builder.build();
  }

  @Override
  public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
    Block texture = level.getModelData(pos).get(RetexturedHelper.BLOCK_PROPERTY);
    return texture != null && texture != Blocks.AIR ? RetexturedQuads.materialOf(texture) : delegate.particleMaterial(level, pos, state);
  }

  /** Flags of the wrapped model plus those of the texture: an animated or translucent texture block */
  @Override
  public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
    int flags = delegate.materialFlags(level, pos, state);
    Block texture = level.getModelData(pos).get(RetexturedHelper.BLOCK_PROPERTY);
    if (texture != null && texture != Blocks.AIR) {
      TextureAtlasSprite sprite = RetexturedQuads.spriteOf(texture);
      if (sprite.contents().isAnimated()) {
        flags |= BakedQuad.FLAG_ANIMATED;
      }
      if (sprite.transparency().hasTranslucent()) {
        flags |= BakedQuad.FLAG_TRANSLUCENT;
      }
    }
    return flags;
  }

  @Override
  @Nullable
  public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
    return null;
  }
}
