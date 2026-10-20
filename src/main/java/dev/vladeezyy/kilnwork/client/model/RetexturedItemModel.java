package dev.vladeezyy.kilnwork.client.model;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.retexture.RetexturedHelper;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3fc;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Item model {@code kilnwork:retextured} ({@code {"type": "kilnwork:retextured", "model": "<block model>"}}): the block
 * model with the quads marked by {@link RetexturedModelLoader} on the sprite of the item's texture component.
 */
public class RetexturedItemModel implements ItemModel {
  public static final Identifier ID = Kilnwork.id("retextured");

  private final List<ItemTintSource> tints;
  private final List<BakedQuad> quads;
  private final Supplier<Vector3fc[]> extents;
  private final ModelRenderProperties properties;
  /** Quads per texture block, made once */
  private final Map<Block, List<BakedQuad>> retextured = new ConcurrentHashMap<>();

  public RetexturedItemModel(List<ItemTintSource> tints, List<BakedQuad> quads, ModelRenderProperties properties) {
    this.tints = tints;
    this.quads = quads;
    this.properties = properties;
    this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(quads));
  }

  @Override
  public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
    Block texture = RetexturedHelper.getTexture(stack);
    state.appendModelIdentityElement(this);
    state.appendModelIdentityElement(texture);
    ItemStackRenderState.LayerRenderState layer = state.newLayer();
    if (stack.hasFoil()) {
      layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
      state.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
    }
    int[] tintLayers = layer.prepareTintLayers(tints.size());
    for (int i = 0; i < tints.size(); i++) {
      int tint = tints.get(i).calculate(stack, level, owner == null ? null : owner.asLivingEntity());
      tintLayers[i] = tint;
      state.appendModelIdentityElement(tint);
    }
    layer.setExtents(extents);
    layer.setRenderType(renderType(stack));
    properties.applyToLayer(layer, context);
    List<BakedQuad> drawn = texture == Blocks.AIR ? quads
      : retextured.computeIfAbsent(texture, block -> RetexturedQuads.retexture(quads, RetexturedQuads.spriteOf(block)));
    layer.prepareQuadList().addAll(drawn);
  }

  /** Cutout block sheet, translucent for blocks drawn in the translucent layer (as vanilla block items) */
  private static RenderType renderType(ItemStack stack) {
    if (stack.getItem() instanceof BlockItem item
        && ItemBlockRenderTypes.getChunkRenderType(item.getBlock().defaultBlockState()) != ChunkSectionLayer.TRANSLUCENT) {
      return Sheets.cutoutBlockSheet();
    }
    return Sheets.translucentBlockItemSheet();
  }

  public record Unbaked(Identifier model, List<ItemTintSource> tints) implements ItemModel.Unbaked {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
      ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(Unbaked::tints)
    ).apply(inst, Unbaked::new));

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context) {
      ModelBaker baker = context.blockModelBaker();
      ResolvedModel resolved = baker.getModel(model);
      TextureSlots textures = resolved.getTopTextureSlots();
      List<BakedQuad> quads = resolved.bakeTopGeometry(textures, baker, BlockModelRotation.IDENTITY).getAll();
      return new RetexturedItemModel(tints, quads, ModelRenderProperties.fromResolvedModel(baker, resolved, textures));
    }

    @Override
    public MapCodec<Unbaked> type() {
      return MAP_CODEC;
    }
  }
}
