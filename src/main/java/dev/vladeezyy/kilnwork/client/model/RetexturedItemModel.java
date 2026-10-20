package dev.vladeezyy.kilnwork.client.model;

import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.retexture.RetexturedHelper;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4fc;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Item model {@code kilnwork:retextured} ({@code {"type": "kilnwork:retextured", "model": "<block model>"}}): the block
 * model with the quads marked by {@link RetexturedModelLoader} on the sprite of the item's texture component. One
 * vanilla cuboid item model per texture block, made once; the layer and item render type of each quad follow its
 * sprite (26.1).
 */
public class RetexturedItemModel implements ItemModel {
  public static final Identifier ID = Kilnwork.id("retextured");

  private final List<ItemTintSource> tints;
  private final QuadCollection quads;
  private final ModelRenderProperties properties;
  private final Matrix4fc transformation;
  /** Model per texture block */
  private final Map<Block, ItemModel> models = new ConcurrentHashMap<>();

  public RetexturedItemModel(List<ItemTintSource> tints, QuadCollection quads, ModelRenderProperties properties, Matrix4fc transformation) {
    this.tints = tints;
    this.quads = quads;
    this.properties = properties;
    this.transformation = transformation;
  }

  private ItemModel bake(Block texture) {
    QuadCollection drawn = quads;
    if (texture != Blocks.AIR) {
      TextureAtlasSprite sprite = RetexturedQuads.spriteOf(texture);
      drawn = RetexturedQuads.transform(quads, quad -> true, quad -> RetexturedQuads.isRetextured(quad) ? RetexturedQuads.retexture(quad, sprite) : quad);
    }
    return new CuboidItemModelWrapper(tints, drawn, properties, transformation);
  }

  @Override
  public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
    Block texture = RetexturedHelper.getTexture(stack);
    state.appendModelIdentityElement(this);
    models.computeIfAbsent(texture, this::bake).update(state, stack, resolver, context, level, owner, seed);
  }

  public record Unbaked(Identifier model, Optional<Transformation> transformation, List<ItemTintSource> tints) implements ItemModel.Unbaked {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
      Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation),
      ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(Unbaked::tints)
    ).apply(inst, Unbaked::new));

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
      ModelBaker baker = context.blockModelBaker();
      ResolvedModel resolved = baker.getModel(model);
      TextureSlots textures = resolved.getTopTextureSlots();
      QuadCollection quads = resolved.bakeTopGeometry(textures, baker, BlockModelRotation.IDENTITY);
      return new RetexturedItemModel(tints, quads, ModelRenderProperties.fromResolvedModel(baker, resolved, textures),
        Transformation.compose(transformation, this.transformation));
    }

    @Override
    public MapCodec<Unbaked> type() {
      return MAP_CODEC;
    }
  }
}
