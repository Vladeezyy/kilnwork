package dev.vladeezyy.kilnwork.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vladeezyy.kilnwork.data.BlockStateDataLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3f;

import java.util.List;

/**
 * Where a block entity renderer draws an item: center and size in pixels, rotation, display context. Lists per block
 * state are read from {@code assets/<namespace>/kilnwork/item_lists/<block>.json} ({@link #ITEM_LISTS}).
 * Based on Mantle's RenderItem (MIT, SlimeKnights).
 */
public record RenderItem(Vector3f center, float size, int x, int y, ItemDisplayContext transform) {
  private static final Codec<Integer> ROTATION = Codec.INT.validate(r -> r == 0 || r == 90 || r == 180 || r == 270 ? DataResult.success(r) : DataResult.error(() -> "Rotation must be 0, 90, 180 or 270: " + r));
  public static final Codec<RenderItem> CODEC = RecordCodecBuilder.create(inst -> inst.group(
    ExtraCodecs.VECTOR3F.optionalFieldOf("center", new Vector3f()).forGetter(RenderItem::center),
    Codec.floatRange(0, Float.MAX_VALUE).fieldOf("size").forGetter(RenderItem::size),
    ROTATION.optionalFieldOf("x", 0).forGetter(RenderItem::x),
    ROTATION.optionalFieldOf("y", 0).forGetter(RenderItem::y),
    ItemDisplayContext.CODEC.optionalFieldOf("transform", ItemDisplayContext.NONE).forGetter(RenderItem::transform)
  ).apply(inst, RenderItem::new));
  /** Item positions per block state */
  public static final BlockStateDataLoader<List<RenderItem>> ITEM_LISTS = BlockStateDataLoader.of("Block entity items", "kilnwork/item_lists", CODEC.listOf());

  /** True if nothing is drawn here */
  public boolean isHidden() {
    return size == 0;
  }

  /** Draws the item at this position */
  public void render(PoseStack matrices, MultiBufferSource buffer, ItemStack item, int light) {
    if (isHidden() || item.isEmpty()) {
      return;
    }
    matrices.pushPose();
    matrices.translate(center.x() / 16f, center.y() / 16f, center.z() / 16f);
    float scale = size / 16f;
    matrices.scale(scale, scale, scale);
    if (x != 0) {
      matrices.mulPose(Axis.XP.rotationDegrees(x));
    }
    if (y != 0) {
      matrices.mulPose(Axis.YP.rotationDegrees(y));
    }
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.getItemRenderer().renderStatic(item, transform, light, OverlayTexture.NO_OVERLAY, matrices, buffer, minecraft.level, 0);
    matrices.popPose();
  }

  /**
   * Rotates the renderer to the horizontal facing of the state, south is unrotated
   * @return true if a pose was pushed, pop it after drawing
   */
  public static boolean applyRotation(PoseStack matrices, BlockState state) {
    if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
      return false;
    }
    Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    if (facing == Direction.SOUTH) {
      return false;
    }
    matrices.pushPose();
    matrices.translate(0.5, 0, 0.5);
    matrices.mulPose(Axis.YP.rotationDegrees(-90f * facing.get2DDataValue()));
    matrices.translate(-0.5, 0, -0.5);
    return true;
  }
}
