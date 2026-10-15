package dev.vladeezyy.kilnwork.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.vladeezyy.kilnwork.client.render.FluidCuboid.FluidFace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws fluid cuboids in block entity renderers. Based on Mantle's FluidRenderer (MIT, SlimeKnights), with texture
 * coordinates for 1.21 (0-1 sprite fractions) and the vanilla translucent item entity render type. Since 1.21.9 block
 * entity renderers submit geometry to a {@link SubmitNodeCollector} instead of drawing directly.
 */
public final class FluidCuboidRenderer {
  private FluidCuboidRenderer() {}

  /** Render type fluids are drawn with */
  public static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);

  /** Sprite of the block atlas */
  public static TextureAtlasSprite getBlockSprite(Identifier sprite) {
    return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(sprite);
  }

  /** Raises the block light of the packed light to at least the given 0-15 value, for glowing fluids */
  public static int withBlockLight(int packedLight, int blockLight) {
    return (packedLight & 0xFFFF0000) | Math.max(blockLight << 4, packedLight & 0xFFFF);
  }

  /**
   * Rotates the pose around the block center so south becomes the given horizontal direction
   * @return  True if a pose was pushed, the caller must pop it
   */
  public static boolean applyRotation(PoseStack matrices, Direction facing) {
    if (facing.getAxis().isHorizontal() && facing != Direction.SOUTH) {
      matrices.pushPose();
      matrices.translate(0.5, 0, 0.5);
      matrices.rotateDegrees(Axis.YP, -90f * facing.get2DDataValue());
      matrices.translate(-0.5, 0, -0.5);
      return true;
    }
    return false;
  }

  /**
   * Submits a fluid cuboid between the given points (block units) at the current pose
   * @param isGas  Flips the textures
   */
  public static void submitCuboid(PoseStack matrices, SubmitNodeCollector collector, FluidCuboid cube, TextureAtlasSprite still, TextureAtlasSprite flowing, Vector3f from, Vector3f to, int color, int light, boolean isGas) {
    Vector3f start = new Vector3f(from);
    Vector3f end = new Vector3f(to);
    collector.submitCustomGeometry(matrices, RENDER_TYPE, (pose, buffer) -> renderCuboid(pose, buffer, cube, still, flowing, start, end, color, light, isGas));
  }

  /** Submits a fluid cuboid at its own size, moved up or down by the offset */
  public static void submitCuboid(PoseStack matrices, SubmitNodeCollector collector, FluidCuboid cube, float yOffset, TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light, boolean isGas) {
    if (yOffset != 0) {
      matrices.pushPose();
      matrices.translate(0, yOffset, 0);
    }
    submitCuboid(matrices, collector, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light, isGas);
    if (yOffset != 0) {
      matrices.popPose();
    }
  }

  /** Draws the faces of a cuboid into a buffer */
  public static void renderCuboid(PoseStack.Pose pose, VertexConsumer buffer, FluidCuboid cube, TextureAtlasSprite still, TextureAtlasSprite flowing, Vector3f from, Vector3f to, int color, int light, boolean isGas) {
    int rotation = isGas ? 180 : 0;
    for (Direction dir : Direction.values()) {
      FluidFace face = cube.getFace(dir);
      if (face != null) {
        boolean isFlowing = face.isFlowing();
        int faceRot = (rotation + face.rotation()) % 360;
        putTexturedQuad(buffer, pose, isFlowing ? flowing : still, from, to, dir, color, light, faceRot, isFlowing);
      }
    }
  }

  /**
   * Forces the UV to be between 0 and 1
   * @param value  Original value
   * @param upper  If true, this is the larger UV. Needed to enforce integer values end up at 1
   * @return  UV mapped between 0 and 1
   */
  private static float boundUV(float value, boolean upper) {
    value = value % 1;
    if (value == 0) {
      // if it lands exactly on the 0 bound, map that to 1 instead for the larger UV
      return upper ? 1 : 0;
    }
    // modulo returns a negative result if the input is negative, so add 1 to account for that
    return value < 0 ? (value + 1) : value;
  }

  /**
   * Adds a quad to the renderer
   * @param renderer    Renderer instnace
   * @param matrix      Render matrix
   * @param sprite      Sprite to render
   * @param from        Quad start
   * @param to          Quad end
   * @param face        Face to render
   * @param color       Color to use in rendering
   * @param brightness  Face brightness
   * @param flowing     If true, half texture coordinates
   */
  public static void putTexturedQuad(VertexConsumer renderer, PoseStack.Pose pose, TextureAtlasSprite sprite, Vector3f from, Vector3f to, Direction face, int color, int brightness, int rotation, boolean flowing) {
    Matrix4f matrix = pose.pose();
    // start with texture coordinates
    float x1 = from.x(), y1 = from.y(), z1 = from.z();
    float x2 = to.x(), y2 = to.y(), z2 = to.z();
    // choose UV based on the directions, some need to negate UV due to the direction
    // note that we use -UV instead of 1-UV as its slightly simpler and the later logic deals with negatives
    float u1, u2, v1, v2;
    switch (face) {
      default -> { // DOWN
        u1 = x1; u2 = x2;
        v1 = z2; v2 = z1;
      }
      case UP -> {
        u1 = x1; u2 = x2;
        v1 = -z1; v2 = -z2;
      }
      case NORTH -> {
        u1 = -x1; u2 = -x2;
        v1 = y1; v2 = y2;
      }
      case SOUTH -> {
        u1 = x2; u2 = x1;
        v1 = y1; v2 = y2;
      }
      case WEST -> {
        u1 = z2; u2 = z1;
        v1 = y1; v2 = y2;
      }
      case EAST -> {
        u1 = -z1; u2 = -z2;
        v1 = y1; v2 = y2;
      }
    }

    // flip V when relevant
    if (rotation == 0 || rotation == 270) {
      float temp = v1;
      v1 = -v2;
      v2 = -temp;
    }
    // flip U when relevant
    if (rotation >= 180) {
      float temp = u1;
      u1 = -u2;
      u2 = -temp;
    }

    // bound UV to be between 0 and 1
    boolean reverse = u1 > u2;
    u1 = boundUV(u1, reverse);
    u2 = boundUV(u2, !reverse);
    reverse = v1 > v2;
    v1 = boundUV(v1, reverse);
    v2 = boundUV(v2, !reverse);

    // if rotating by 90 or 270, swap U and V
    float minU, maxU, minV, maxV;
    // 1.21: getU and getV take a 0-1 fraction of the sprite, Mantle 1.21.1-1.12.10 still passes 0-16 (1.20)
    float size = flowing ? 0.5f : 1f;
    if ((rotation % 180) == 90) {
      minU = sprite.getU(v1 * size);
      maxU = sprite.getU(v2 * size);
      minV = sprite.getV(u1 * size);
      maxV = sprite.getV(u2 * size);
    } else {
      minU = sprite.getU(u1 * size);
      maxU = sprite.getU(u2 * size);
      minV = sprite.getV(v1 * size);
      maxV = sprite.getV(v2 * size);
    }
    // based on rotation, put coords into place
    float u3, u4, v3, v4;
    switch(rotation) {
      default -> { // 0
        u1 = minU; v1 = maxV;
        u2 = minU; v2 = minV;
        u3 = maxU; v3 = minV;
        u4 = maxU; v4 = maxV;
      }
      case 90 -> {
        u1 = minU; v1 = minV;
        u2 = maxU; v2 = minV;
        u3 = maxU; v3 = maxV;
        u4 = minU; v4 = maxV;
      }
      case 180 -> {
        u1 = maxU; v1 = minV;
        u2 = maxU; v2 = maxV;
        u3 = minU; v3 = maxV;
        u4 = minU; v4 = minV;
      }
      case 270 -> {
        u1 = maxU; v1 = maxV;
        u2 = minU; v2 = maxV;
        u3 = minU; v3 = minV;
        u4 = maxU; v4 = minV;
      }
    }
    // add quads
    float nx = face.getStepX(), ny = face.getStepY(), nz = face.getStepZ();
    int a = color >> 24 & 0xFF;
    int r = color >> 16 & 0xFF;
    int g = color >> 8 & 0xFF;
    int b = color & 0xFF;
    switch (face) {
      case DOWN -> {
        renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
      }
      case UP -> {
        renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u4, v4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
      }
      case NORTH -> {
        renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u4, v4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
      }
      case SOUTH -> {
        renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
      }
      case WEST -> {
        renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u4, v4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
      }
      case EAST -> {
        renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
        renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(brightness).setNormal(pose, nx, ny, nz);
      }
    }
  }

  /**
   * Collects the fluid cuboids below a faucet or channel, from {@link KilnworkFluidModels#FAUCET_FLUIDS}. Call while
   * extracting the render state, the level is not available when submitting.
   * @return  Cuboids for each block below, index 0 is one block down
   */
  public static List<List<FluidCuboid>> collectFaucetFluids(BlockGetter level, BlockPos pos, Direction direction) {
    List<List<FluidCuboid>> list = new ArrayList<>();
    FaucetFluid faucetFluid;
    int i = 0;
    do {
      i++;
      faucetFluid = KilnworkFluidModels.FAUCET_FLUIDS.get(level.getBlockState(pos.below(i)));
      list.add(faucetFluid.getFluids(direction));
    } while (faucetFluid.isContinued() && i < 16);
    return list;
  }

  /** Submits the fluid collected by {@link #collectFaucetFluids(BlockGetter, BlockPos, Direction)} */
  public static void submitFaucetFluids(List<List<FluidCuboid>> fluids, PoseStack matrices, SubmitNodeCollector collector, TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light) {
    for (int i = 0; i < fluids.size(); i++) {
      matrices.pushPose();
      matrices.translate(0, -(i + 1), 0);
      for (FluidCuboid cube : fluids.get(i)) {
        submitCuboid(matrices, collector, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light, false);
      }
      matrices.popPose();
    }
  }
}
