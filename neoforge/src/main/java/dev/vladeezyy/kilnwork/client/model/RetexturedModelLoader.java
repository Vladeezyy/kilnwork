package dev.vladeezyy.kilnwork.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import dev.vladeezyy.kilnwork.Kilnwork;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.DelegateUnbakedModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loader {@code kilnwork:retextured}: a vanilla block model plus {@code "retextured": [texture names]}. The quads using
 * those textures take the texture of the block in the block entity ({@link RetexturedBlockModel}) or on the item
 * ({@link RetexturedItemModel}). Elements may come from the parent. Based on Mantle's RetexturedModel (MIT, SlimeKnights).
 */
public class RetexturedModelLoader implements UnbakedModelLoader<UnbakedModel> {
  public static final Identifier ID = Kilnwork.id("retextured");
  public static final RetexturedModelLoader INSTANCE = new RetexturedModelLoader();

  @Override
  public UnbakedModel read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
    Set<String> names = getRetexturedNames(json);
    CuboidModel model = readVanilla(json, context);
    return new Wrapped(model, new Geometry(model.geometry(), model.parent(), names));
  }

  /** The JSON read as a vanilla block model, without the loader key */
  public static CuboidModel readVanilla(JsonObject json, JsonDeserializationContext context) {
    JsonObject copy = json.deepCopy();
    copy.remove("loader");
    return context.deserialize(copy, CuboidModel.class);
  }

  /** Reads {@code "retextured"}: a texture name or a list of names */
  public static Set<String> getRetexturedNames(JsonObject json) {
    if (!json.has("retextured")) {
      throw new JsonSyntaxException("Missing retextured, expected a string or an array");
    }
    JsonElement element = json.get("retextured");
    if (element.isJsonArray()) {
      JsonArray array = element.getAsJsonArray();
      if (array.isEmpty()) {
        throw new JsonSyntaxException("retextured needs at least one texture");
      }
      List<String> names = new ArrayList<>(array.size());
      for (int i = 0; i < array.size(); i++) {
        names.add(GsonHelper.convertToString(array.get(i), "retextured[" + i + "]"));
      }
      return Set.copyOf(names);
    }
    return Set.of(GsonHelper.convertToString(element, "retextured"));
  }

  /** Vanilla model with the retextured geometry */
  public static class Wrapped extends DelegateUnbakedModel {
    private final UnbakedGeometry geometry;

    public Wrapped(UnbakedModel delegate, UnbakedGeometry geometry) {
      super(delegate);
      this.geometry = geometry;
    }

    @Override
    public UnbakedGeometry geometry() {
      return geometry;
    }
  }

  /** Geometry of the model (or of its parent), marking the quads of the retextured textures */
  public static class Geometry implements UnbakedGeometry {
    @Nullable
    private final UnbakedGeometry own;
    @Nullable
    private final Identifier parent;
    private final Set<String> names;

    public Geometry(@Nullable UnbakedGeometry own, @Nullable Identifier parent, Set<String> names) {
      this.own = own;
      this.parent = parent;
      this.names = names;
    }

    /** Geometry of the model, else the first one up the parents */
    protected UnbakedGeometry base(ModelBaker baker) {
      if (own != null) {
        return own;
      }
      return parent != null ? baker.getModel(parent).getTopGeometry() : UnbakedGeometry.EMPTY;
    }

    @Override
    public QuadCollection bake(TextureSlots textures, ModelBaker baker, ModelState state, ModelDebugName name) {
      QuadCollection quads = bakeQuads(textures, baker, state, name);
      Set<TextureAtlasSprite> sprites = new HashSet<>();
      for (String texture : names) {
        Material material = textures.getMaterial(texture);
        if (material != null) {
          sprites.add(baker.materials().get(material, name).sprite());
        }
      }
      RetexturedQuads.markRetextured(quads, sprites);
      return quads;
    }

    /** Bakes the quads, override to add more (marked with {@link RetexturedQuads#markGroup}) */
    protected QuadCollection bakeQuads(TextureSlots textures, ModelBaker baker, ModelState state, ModelDebugName name) {
      return base(baker).bake(textures, baker, state, name);
    }
  }
}
