package dev.vladeezyy.kilnwork.client.model;

import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.neoforged.neoforge.client.model.pipeline.TransformingVertexPipeline;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Small model helpers */
public final class ModelHelpers {
  private ModelHelpers() {}

  private static final Map<Block, ResourceLocation> PARTICLES = new ConcurrentHashMap<>();

  /** Particle texture of the default state model of the block, cached */
  public static ResourceLocation getParticleTexture(Block block) {
    return PARTICLES.computeIfAbsent(block, b -> {
      TextureAtlasSprite particle = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(b.defaultBlockState()).getParticleIcon();
      //noinspection ConstantConditions  mods may return null
      return particle != null ? particle.contents().name() : MissingTextureAtlasSprite.getLocation();
    });
  }

  /** Clears cached data on resource reload */
  public static void clearCache() {
    PARTICLES.clear();
  }

  /** Quad transformer setting a fixed ARGB color on every vertex */
  public static IQuadTransformer color(int argb) {
    int abgr = (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb << 16) & 0xFF0000);
    return quad -> {
      int[] vertices = quad.getVertices();
      for (int i = 0; i < 4; i++) {
        vertices[i * IQuadTransformer.STRIDE + IQuadTransformer.COLOR] = abgr;
      }
    };
  }

  /**
   * One flat quad of a sprite facing south at the middle of the item, as seen in GUIs
   * @param color      ARGB color, -1 for none
   * @param transform  Transform to apply
   */
  public static BakedQuad flatItemQuad(int color, TextureAtlasSprite sprite, Transformation transform) {
    QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
    builder.setSprite(sprite);
    builder.setTintIndex(-1);
    builder.setShade(false);
    builder.setHasAmbientOcclusion(true);
    builder.setDirection(Direction.SOUTH);
    com.mojang.blaze3d.vertex.VertexConsumer consumer = transform.isIdentity() ? builder : new TransformingVertexPipeline(builder, transform);
    float z = 8.5f / 16f;
    float[][] corners = {{0, 0, sprite.getU0(), sprite.getV1()}, {1, 0, sprite.getU1(), sprite.getV1()}, {1, 1, sprite.getU1(), sprite.getV0()}, {0, 1, sprite.getU0(), sprite.getV0()}};
    for (float[] corner : corners) {
      consumer.addVertex(corner[0], corner[1], z).setColor(color).setUv(corner[2], corner[3]).setNormal(0, 0, 1);
    }
    return builder.bakeQuad();
  }
}
