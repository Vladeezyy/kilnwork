package dev.vladeezyy.kilnwork.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Quads of retextured models, marked while the models bake: which quads take the texture block's sprite, and the group
 * of a quad (for example a book of a shelf). Retexturing moves the UVs of a marked quad from its sprite onto the
 * texture's sprite, so no model is baked again at runtime (Mantle baked a model per texture).
 */
public final class RetexturedQuads {
  private RetexturedQuads() {}

  private static final Set<BakedQuad> RETEXTURED = ConcurrentHashMap.newKeySet();
  private static final Map<BakedQuad, Integer> GROUPS = new ConcurrentHashMap<>();
  /** Set after the models baked: the next model that marks quads starts a new generation */
  private static volatile boolean stale;

  /** Called when the models finished baking (client mod bus) */
  public static void bakingCompleted() {
    stale = true;
  }

  private static void startGeneration() {
    if (stale) {
      synchronized (RetexturedQuads.class) {
        if (stale) {
          RETEXTURED.clear();
          GROUPS.clear();
          stale = false;
        }
      }
    }
  }

  /** Marks the quads of the collection using one of the sprites as retextured */
  public static void markRetextured(QuadCollection quads, Set<TextureAtlasSprite> sprites) {
    startGeneration();
    if (sprites.isEmpty()) {
      return;
    }
    for (BakedQuad quad : quads.getAll()) {
      if (sprites.contains(quad.materialInfo().sprite())) {
        RETEXTURED.add(quad);
      }
    }
  }

  /** Puts every quad of the collection in the group */
  public static void markGroup(QuadCollection quads, int group) {
    startGeneration();
    for (BakedQuad quad : quads.getAll()) {
      GROUPS.put(quad, group);
    }
  }

  public static boolean isRetextured(BakedQuad quad) {
    return RETEXTURED.contains(quad);
  }

  /** Group of the quad, -1 if none */
  public static int group(BakedQuad quad) {
    return GROUPS.getOrDefault(quad, -1);
  }

  /** Material drawn for a texture block: its particle material */
  public static Material.Baked materialOf(Block block) {
    return Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(block.defaultBlockState());
  }

  /** Sprite drawn for a texture block: its particle sprite */
  public static TextureAtlasSprite spriteOf(Block block) {
    return materialOf(block).sprite();
  }

  /**
   * The quad with its UVs moved from its own sprite onto the given one. Since 26.1 the layer (solid, cutout, translucent)
   * comes from the sprite's pixels under the quad, so it is computed again for the new sprite.
   */
  public static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite sprite) {
    BakedQuad.MaterialInfo info = quad.materialInfo();
    TextureAtlasSprite old = info.sprite();
    if (old == sprite) {
      return quad;
    }
    long[] uv = new long[4];
    float minU = 1, minV = 1, maxU = 0, maxV = 0;
    for (int i = 0; i < 4; i++) {
      long packed = quad.packedUV(i);
      float u = (UVPair.unpackU(packed) - old.getU0()) / (old.getU1() - old.getU0());
      float v = (UVPair.unpackV(packed) - old.getV0()) / (old.getV1() - old.getV0());
      minU = Math.min(minU, u);
      minV = Math.min(minV, v);
      maxU = Math.max(maxU, u);
      maxV = Math.max(maxV, v);
      uv[i] = UVPair.pack(sprite.getU(u), sprite.getV(v));
    }
    Transparency transparency = info.layer().translucent() ? Transparency.TRANSLUCENT : sprite.contents().computeTransparency(minU, minV, maxU, maxV);
    BakedQuad.MaterialInfo retextured = BakedQuad.MaterialInfo.of(new Material.Baked(sprite, false), transparency, info.tintIndex(), info.shadeDirectionOverride(), info.lightEmission(), info.ambientOcclusion());
    return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(), uv[0], uv[1], uv[2], uv[3],
      quad.direction(), retextured, quad.bakedNormals(), quad.bakedColors());
  }

  /** Retextures the marked quads of the list, null keeps them */
  public static List<BakedQuad> retexture(List<BakedQuad> quads, TextureAtlasSprite sprite) {
    return transform(quads, quad -> true, quad -> isRetextured(quad) ? retexture(quad, sprite) : quad);
  }

  /** The quads passing the filter, transformed */
  public static List<BakedQuad> transform(List<BakedQuad> quads, Predicate<BakedQuad> keep, UnaryOperator<BakedQuad> transform) {
    if (quads.isEmpty()) {
      return quads;
    }
    BakedQuad[] out = new BakedQuad[quads.size()];
    int n = 0;
    for (BakedQuad quad : quads) {
      if (keep.test(quad)) {
        out[n++] = transform.apply(quad);
      }
    }
    return n == 0 ? List.of() : Collections.unmodifiableList(java.util.Arrays.asList(java.util.Arrays.copyOf(out, n)));
  }

  /** The collection with each quad filtered and transformed, sides kept */
  public static QuadCollection transform(QuadCollection quads, Predicate<BakedQuad> keep, UnaryOperator<BakedQuad> transform) {
    QuadCollection.Builder builder = new QuadCollection.Builder();
    for (BakedQuad quad : quads.getQuads(null)) {
      if (keep.test(quad)) {
        builder.addUnculledFace(transform.apply(quad));
      }
    }
    for (Direction side : Direction.values()) {
      for (BakedQuad quad : quads.getQuads(side)) {
        if (keep.test(quad)) {
          builder.addCulledFace(side, transform.apply(quad));
        }
      }
    }
    return builder.build();
  }
}
