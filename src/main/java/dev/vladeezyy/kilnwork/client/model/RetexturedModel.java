package dev.vladeezyy.kilnwork.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.retexture.RetexturedHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.geometry.BlockGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Model replacing the textures named in {@code "retextured"} with the particle texture of the block in
 * {@link RetexturedHelper#BLOCK_PROPERTY} (blocks) or the texture component (items). Loader {@code kilnwork:retextured}.
 * Based on Mantle's RetexturedModel (MIT, SlimeKnights).
 */
public class RetexturedModel implements IUnbakedGeometry<RetexturedModel> {
  public static final ResourceLocation ID = Kilnwork.id("retextured");
  public static final IGeometryLoader<RetexturedModel> LOADER = RetexturedModel::deserialize;

  private final SimpleBlockModel model;
  private final Set<String> retextured;

  protected RetexturedModel(SimpleBlockModel model, Set<String> retextured) {
    this.model = model;
    this.retextured = retextured;
  }

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
    model.resolveParents(modelGetter, context);
  }

  @Override
  public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides) {
    BakedModel baked = model.bake(owner, baker, spriteGetter, transform, overrides);
    return new Baked(baked, owner, model, transform, getAllRetextured(owner, model, retextured));
  }

  public static RetexturedModel deserialize(JsonObject json, JsonDeserializationContext context) {
    return new RetexturedModel(SimpleBlockModel.deserialize(json, context), getRetexturedNames(json));
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

  /** The retextured names plus every texture of the model or its parents that refers to one of them */
  public static Set<String> getAllRetextured(IGeometryBakingContext owner, SimpleBlockModel model, Set<String> names) {
    Set<String> all = new HashSet<>(names);
    List<Map<String, Either<Material, String>>> maps = new ArrayList<>();
    if (owner instanceof BlockGeometryBakingContext blockOwner) {
      for (BlockModel link = blockOwner.owner; link != null; link = link.parent) {
        maps.add(link.textureMap);
      }
    } else {
      maps.add(model.getTextures());
      for (BlockModel link = model.getParent(); link != null; link = link.parent) {
        maps.add(link.textureMap);
      }
    }
    for (Map<String, Either<Material, String>> textures : maps) {
      textures.forEach((name, either) -> either.ifRight(reference -> {
        if (all.contains(reference)) {
          all.add(name);
        }
      }));
    }
    return Set.copyOf(all);
  }

  /** Context using the given texture for each of the names */
  public static IGeometryBakingContext retexture(IGeometryBakingContext owner, Set<String> names, ResourceLocation texture) {
    Material material = new Material(InventoryMenu.BLOCK_ATLAS, texture);
    Map<String, Material> map = new HashMap<>();
    for (String name : names) {
      map.put(name, material);
    }
    return new ExtraTextureContext(owner, map);
  }

  /** Baked model swapping in the cached model of each texture */
  public static class Baked extends DynamicBakedWrapper<BakedModel> {
    private final Map<ResourceLocation, BakedModel> cache = new ConcurrentHashMap<>();
    private final IGeometryBakingContext owner;
    private final SimpleBlockModel model;
    private final ModelState transform;
    private final Set<String> retextured;
    private final ItemOverrides overrides = new ItemOverrides() {
      @Nullable
      @Override
      public BakedModel resolve(BakedModel original, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (stack.isEmpty()) {
          return original;
        }
        Block block = RetexturedHelper.getTexture(stack);
        return block == Blocks.AIR ? original : getCachedModel(block);
      }
    };

    public Baked(BakedModel baked, IGeometryBakingContext owner, SimpleBlockModel model, ModelState transform, Set<String> retextured) {
      super(baked);
      this.owner = owner;
      this.model = model;
      this.transform = transform;
      this.retextured = retextured;
    }

    private BakedModel getCachedModel(Block block) {
      return cache.computeIfAbsent(ModelHelpers.getParticleTexture(block), texture -> model.bakeDynamic(retexture(owner, retextured, texture), transform));
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
      if (retextured.contains("particle")) {
        Block block = data.get(RetexturedHelper.BLOCK_PROPERTY);
        if (block != null) {
          return getCachedModel(block).getParticleIcon(data);
        }
      }
      return originalModel.getParticleIcon(data);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
      Block block = data.get(RetexturedHelper.BLOCK_PROPERTY);
      if (block == null) {
        return originalModel.getQuads(state, side, rand, data, renderType);
      }
      return getCachedModel(block).getQuads(state, side, rand, data, renderType);
    }

    @Override
    public ItemOverrides getOverrides() {
      return overrides;
    }
  }
}
