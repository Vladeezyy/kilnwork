package dev.vladeezyy.kilnwork.client.render;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Box of fluid inside a block, in pixels (0-16), with the faces to draw.
 * JSON: {@code {"from": [x, y, z], "to": [x, y, z], "faces": {"up": {"flowing": false, "rotation": 0}}}}, no faces means all.
 */
public final class FluidCuboid {
  private static final Map<Direction, FluidFace> ALL_FACES;
  static {
    ALL_FACES = new EnumMap<>(Direction.class);
    for (Direction direction : Direction.values()) {
      ALL_FACES.put(direction, FluidFace.NORMAL);
    }
  }

  private static final Codec<Vector3f> VECTOR = ExtraCodecs.VECTOR3F.xmap(Vector3f::new, vector -> vector);
  public static final Codec<FluidCuboid> CODEC = RecordCodecBuilder.create(inst -> inst.group(
    VECTOR.fieldOf("from").forGetter(FluidCuboid::getFrom),
    VECTOR.fieldOf("to").forGetter(FluidCuboid::getTo),
    Codec.unboundedMap(Direction.CODEC, FluidFace.CODEC).optionalFieldOf("faces", ALL_FACES).forGetter(FluidCuboid::getFaces)
  ).apply(inst, FluidCuboid::new));
  /** One cuboid or a list of them */
  public static final Codec<List<FluidCuboid>> LIST_CODEC = Codec.either(CODEC.listOf(), CODEC).xmap(
    either -> either.map(list -> list, List::of),
    list -> list.size() == 1 ? Either.right(list.get(0)) : Either.left(list));

  private final Vector3f from;
  private final Vector3f to;
  private final Map<Direction, FluidFace> faces;
  private final Vector3f fromScaled;
  private final Vector3f toScaled;

  public FluidCuboid(Vector3f from, Vector3f to, Map<Direction, FluidFace> faces) {
    this.from = from;
    this.to = to;
    this.faces = faces.isEmpty() ? ALL_FACES : Map.copyOf(faces);
    this.fromScaled = new Vector3f(from).mul(1 / 16f);
    this.toScaled = new Vector3f(to).mul(1 / 16f);
  }

  public Vector3f getFrom() {
    return from;
  }

  public Vector3f getTo() {
    return to;
  }

  public Map<Direction, FluidFace> getFaces() {
    return faces;
  }

  /** Face to draw on the given side, null for none */
  @Nullable
  public FluidFace getFace(Direction direction) {
    return faces.get(direction);
  }

  /** Start in block units (0-1), do not modify */
  public Vector3f getFromScaled() {
    return fromScaled;
  }

  /** End in block units (0-1), do not modify */
  public Vector3f getToScaled() {
    return toScaled;
  }

  /** Copy with another bottom, used by the {@code "bottom"} shortcut of faucet fluids */
  public FluidCuboid withBottom(float y) {
    return new FluidCuboid(new Vector3f(from.x, y, from.z), to, faces);
  }

  /**
   * Face of a fluid cuboid
   * @param isFlowing  Use the flowing texture instead of the still one
   * @param rotation   Texture rotation, 0, 90, 180 or 270
   */
  public record FluidFace(boolean isFlowing, int rotation) {
    public static final FluidFace NORMAL = new FluidFace(false, 0);
    public static final Codec<FluidFace> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      Codec.BOOL.optionalFieldOf("flowing", false).forGetter(FluidFace::isFlowing),
      Codec.INT.optionalFieldOf("rotation", 0).forGetter(FluidFace::rotation)
    ).apply(inst, FluidFace::new));

    public FluidFace {
      if (rotation < 0 || rotation > 270 || rotation % 90 != 0) {
        throw new IllegalArgumentException("Rotation must be 0, 90, 180 or 270");
      }
    }
  }
}
