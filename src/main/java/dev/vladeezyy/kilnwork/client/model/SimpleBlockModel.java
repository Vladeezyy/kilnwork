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
 * elements, such as cracked overlays. Based on Mantle's SimpleBlockModel (MIT, SlimeKnights).
 */
public class SimpleBlockModel implements IUnbakedGeometry<SimpleBlockModel> {
  @Nullable
  private ResourceLocation parentLocation;
  private final List<BlockElement> parts;
  private final Map<String, Either<Material, String>> textures;
  @Nullable
  private BlockModel parent;

  public SimpleBlockModel(@Nullable ResourceLocation parentLocation, Map<String, Either<Material, String>> textures, List<BlockElement> parts) {
    this.parentLocation = parentLocation;
    this.textures = textures;
    this.parts = parts;
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
    TextureAtlasSprite particle = spriteGetter.apply(owner.getMaterial("particle"));
    SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(owner.useAmbientOcclusion(), owner.useBlockLight(), owner.isGui3d(), owner.getTransforms(), overrides).particle(particle);
    IQuadTransformer quadTransformer = owner.getRootTransform().isIdentity() ? QuadTransformers.empty() : UnbakedGeometryHelper.applyRootTransform(transform, owner.getRootTransform());
    for (BlockElement part : elements) {
      for (Map.Entry<Direction, BlockElementFace> entry : part.faces.entrySet()) {
        Direction direction = entry.getKey();
        BlockElementFace face = entry.getValue();
        String texture = face.texture();
        if (texture.charAt(0) == '#') {
          texture = texture.substring(1);
        }
        BakedQuad quad = BlockModel.bakeFace(part, face, spriteGetter.apply(owner.getMaterial(texture)), direction, transform);
        quadTransformer.processInPlace(quad);
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
    return bakeModel(owner, getElements(), spriteGetter, transform, overrides);
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
    List<BlockElement> parts = new ArrayList<>();
    if (json.has("elements")) {
      JsonElement elements = json.get("elements");
      if (elements instanceof JsonArray array) {
        for (JsonElement element : array) {
          parts.add(context.deserialize(element, BlockElement.class));
        }
      } else if (elements.isJsonObject()) {
        parts.add(context.deserialize(elements, BlockElement.class));
      } else {
        throw new JsonSyntaxException("Expected elements to be an array or object");
      }
    }
    return new SimpleBlockModel(parent, Map.copyOf(textures), List.copyOf(parts));
  }
}
