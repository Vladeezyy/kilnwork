package dev.vladeezyy.kilnwork.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import dev.vladeezyy.kilnwork.Kilnwork;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Block model geometry read like a vanilla block model (parent, textures, elements), for loaders that need the
 * elements, such as cracked overlays. Optional {@code "colors": [{"color": "AARRGGBB", "luminosity": 0-15}]} tints
 * the elements by index (loader {@code kilnwork:colored_block}). Based on Mantle's SimpleBlockModel and
 * ColoredBlockModel (MIT, SlimeKnights).
 */
public class SimpleBlockModel implements IUnbakedGeometry<SimpleBlockModel> {
  @Nullable
  private ResourceLocation parentLocation;
  private final List<BlockElement> parts;
  private final Map<String, Either<Material, String>> textures;
  /** Color per element index, missing entries are not tinted */
  private final List<ElementColor> colors;
  @Nullable
  private BlockModel parent;

  public SimpleBlockModel(@Nullable ResourceLocation parentLocation, Map<String, Either<Material, String>> textures, List<BlockElement> parts) {
    this(parentLocation, textures, parts, List.of());
  }

  public SimpleBlockModel(@Nullable ResourceLocation parentLocation, Map<String, Either<Material, String>> textures, List<BlockElement> parts, List<ElementColor> colors) {
    this.parentLocation = parentLocation;
    this.textures = textures;
    this.parts = parts;
    this.colors = colors;
  }

  /** Color and light of one element; color -1 keeps the texture color, luminosity -1 keeps the model light */
  public record ElementColor(int color, int luminosity) {
    public static final ElementColor NONE = new ElementColor(-1, -1);

    /** Reads {@code {"color": "AARRGGBB" or "RRGGBB", "luminosity": 0-15}} */
    public static ElementColor read(JsonObject json) {
      int color = -1;
      if (json.has("color")) {
        color = parseColor(GsonHelper.getAsString(json, "color"));
      }
      int luminosity = GsonHelper.getAsInt(json, "luminosity", -1);
      if (luminosity < -1 || luminosity > 15) {
        throw new JsonSyntaxException("luminosity must be between 0 and 15");
      }
      return new ElementColor(color, luminosity);
    }
  }

  /** Parses a hex color, 6 digits are opaque */
  public static int parseColor(String hex) {
    if (hex.startsWith("#")) {
      hex = hex.substring(1);
    }
    try {
      if (hex.length() == 6) {
        return 0xFF000000 | Integer.parseInt(hex, 16);
      }
      if (hex.length() == 8) {
        return (int) Long.parseLong(hex, 16);
      }
    } catch (NumberFormatException e) {
      // falls through to the error
    }
    throw new JsonSyntaxException("Invalid color '" + hex + "', expected RRGGBB or AARRGGBB");
  }

  /** Parent model after {@link #resolveParents(Function, IGeometryBakingContext)}, null if none */
  @Nullable
  public BlockModel getParent() {
    return parent;
  }

  /** Elements of this model, or of the parent if it has none */
  @SuppressWarnings("deprecation")
  public List<BlockElement> getElements() {
    return parts.isEmpty() && parent != null ? parent.getElements() : parts;
  }

  public Map<String, Either<Material, String>> getTextures() {
    return textures;
  }

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext owner) {
    if (parent != null || parentLocation == null) {
      return;
    }
    Set<UnbakedModel> chain = new LinkedHashSet<>();
    parent = getParent(modelGetter, chain, parentLocation, owner.getModelName());
    if (parent == null) {
      parent = getMissing(modelGetter);
      parentLocation = ModelBakery.MISSING_MODEL_LOCATION;
    }
    for (BlockModel link = parent; link.parentLocation != null && link.parent == null; link = link.parent) {
      chain.add(link);
      link.parent = getParent(modelGetter, chain, link.parentLocation, link.name);
      if (link.parent == null) {
        link.parent = getMissing(modelGetter);
        link.parentLocation = ModelBakery.MISSING_MODEL_LOCATION;
      }
    }
  }

  @Nullable
  private static BlockModel getParent(Function<ResourceLocation, UnbakedModel> modelGetter, Set<UnbakedModel> chain, ResourceLocation location, String name) {
    UnbakedModel unbaked = modelGetter.apply(location);
    if (unbaked == null) {
      Kilnwork.LOG.warn("No parent '{}' while loading model '{}'", location, name);
      return null;
    }
    if (chain.contains(unbaked)) {
      Kilnwork.LOG.warn("Parent loop while loading model '{}'", name);
      return null;
    }
    if (!(unbaked instanceof BlockModel model)) {
      throw new IllegalStateException("Parent of " + name + " has to be a block model");
    }
    return model;
  }

  private static BlockModel getMissing(Function<ResourceLocation, UnbakedModel> modelGetter) {
    if (!(modelGetter.apply(ModelBakery.MISSING_MODEL_LOCATION) instanceof BlockModel model)) {
      throw new IllegalStateException("Failed to load the missing model");
    }
    return model;
  }

  /** Bakes the given elements with the textures of the context */
  public static BakedModel bakeModel(IGeometryBakingContext owner, List<BlockElement> elements, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides) {
    return bakeModel(owner, elements, List.of(), spriteGetter, transform, overrides);
  }

  /** Bakes the given elements with the textures of the context and a color per element */
  public static BakedModel bakeModel(IGeometryBakingContext owner, List<BlockElement> elements, List<ElementColor> colors, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides) {
    TextureAtlasSprite particle = spriteGetter.apply(owner.getMaterial("particle"));
    SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(owner.useAmbientOcclusion(), owner.useBlockLight(), owner.isGui3d(), owner.getTransforms(), overrides).particle(particle);
    IQuadTransformer quadTransformer = owner.getRootTransform().isIdentity() ? QuadTransformers.empty() : UnbakedGeometryHelper.applyRootTransform(transform, owner.getRootTransform());
    for (int i = 0; i < elements.size(); i++) {
      BlockElement part = elements.get(i);
      ElementColor color = i < colors.size() ? colors.get(i) : ElementColor.NONE;
      IQuadTransformer partTransformer = quadTransformer;
      if (color.color() != -1) {
        partTransformer = partTransformer.andThen(ModelHelpers.color(color.color()));
      }
      if (color.luminosity() != -1) {
        partTransformer = partTransformer.andThen(QuadTransformers.settingEmissivity(color.luminosity()));
      }
      for (Map.Entry<Direction, BlockElementFace> entry : part.faces.entrySet()) {
        Direction direction = entry.getKey();
        BlockElementFace face = entry.getValue();
        String texture = face.texture();
        if (texture.charAt(0) == '#') {
          texture = texture.substring(1);
        }
        BakedQuad quad = BlockModel.bakeFace(part, face, spriteGetter.apply(owner.getMaterial(texture)), direction, transform);
        partTransformer.processInPlace(quad);
        Direction cull = face.cullForDirection();
        if (cull == null) {
          builder.addUnculledFace(quad);
        } else {
          builder.addCulledFace(Direction.rotate(transform.getRotation().getMatrix(), cull), quad);
        }
      }
    }
    ResourceLocation renderType = owner.getRenderTypeHint();
    return builder.build(renderType != null ? owner.getRenderType(renderType) : RenderTypeGroup.EMPTY);
  }

  @Override
  public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides) {
    return bakeModel(owner, getElements(), colors, spriteGetter, transform, overrides);
  }

  /**
   * Bakes other elements with this model's colors after the atlas is stitched, such as a shelf with a set of books.
   * Textures come straight from the atlas ({@link Material#sprite()}).
   */
  public BakedModel bakeWithElements(IGeometryBakingContext owner, List<BlockElement> elements, ModelState transform) {
    return bakeModel(owner, elements, colors, Material::sprite, transform, ItemOverrides.EMPTY);
  }

  /** Bakes this model again with another context after the atlas is stitched, such as another texture */
  public BakedModel bakeDynamic(IGeometryBakingContext owner, ModelState transform) {
    return bakeWithElements(owner, getElements(), transform);
  }

  /** Reads one element or an array of elements, as in {@code "elements"} */
  public static List<BlockElement> getModelElements(JsonDeserializationContext context, JsonElement json, String name) {
    List<BlockElement> parts = new ArrayList<>();
    if (json instanceof JsonArray array) {
      for (JsonElement element : array) {
        parts.add(context.deserialize(element, BlockElement.class));
      }
    } else if (json.isJsonObject()) {
      parts.add(context.deserialize(json, BlockElement.class));
    } else {
      throw new JsonSyntaxException("Expected " + name + " to be an array or object");
    }
    return List.copyOf(parts);
  }

  /** Reads the model from JSON */
  public static SimpleBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
    String parentName = GsonHelper.getAsString(json, "parent", "");
    ResourceLocation parent = parentName.isEmpty() ? null : ResourceLocation.parse(parentName);
    Map<String, Either<Material, String>> textures = new HashMap<>();
    if (json.has("textures")) {
      for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "textures").entrySet()) {
        textures.put(entry.getKey(), BlockModel.Deserializer.parseTextureLocationOrReference(InventoryMenu.BLOCK_ATLAS, entry.getValue().getAsString()));
      }
    }
    List<BlockElement> parts = json.has("elements") ? getModelElements(context, json.get("elements"), "elements") : List.of();
    List<ElementColor> colors = new ArrayList<>();
    if (json.has("colors")) {
      for (JsonElement color : GsonHelper.getAsJsonArray(json, "colors")) {
        colors.add(ElementColor.read(GsonHelper.convertToJsonObject(color, "colors")));
      }
    }
    return new SimpleBlockModel(parent, Map.copyOf(textures), parts, List.copyOf(colors));
  }
}
