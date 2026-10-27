package dev.vladeezyy.kilnwork.client.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Fluid shapes of a channel, per block in {@code assets/<namespace>/kilnwork/channel_fluids/}
 * @param down    Stream into the block below
 * @param center  Fluid in the middle
 * @param side    Fluid on a connected side, drawn facing south and rotated
 */
public record ChannelFluids(FluidCuboid down, Center center, Side side) {
  public static final Codec<ChannelFluids> CODEC = RecordCodecBuilder.create(inst -> inst.group(
    FluidCuboid.CODEC.fieldOf("down").forGetter(ChannelFluids::down),
    Center.CODEC.fieldOf("center").forGetter(ChannelFluids::center),
    Side.CODEC.fieldOf("side").forGetter(ChannelFluids::side)
  ).apply(inst, ChannelFluids::new));

  /** Center cuboid, still or flowing to a side */
  public FluidCuboid center(boolean flowing) {
    return center.get(flowing);
  }

  public record Center(FluidCuboid still, FluidCuboid flowing) {
    public static final Codec<Center> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      FluidCuboid.CODEC.fieldOf("still").forGetter(Center::still),
      FluidCuboid.CODEC.fieldOf("flowing").forGetter(Center::flowing)
    ).apply(inst, Center::new));

    public FluidCuboid get(boolean flowing) {
      return flowing ? this.flowing : still;
    }
  }

  public record Side(FluidCuboid still, FluidCuboid edge, FluidCuboid in, FluidCuboid out) {
    public static final Codec<Side> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      FluidCuboid.CODEC.fieldOf("still").forGetter(Side::still),
      FluidCuboid.CODEC.fieldOf("edge").forGetter(Side::edge),
      FluidCuboid.CODEC.fieldOf("in").forGetter(Side::in),
      FluidCuboid.CODEC.fieldOf("out").forGetter(Side::out)
    ).apply(inst, Side::new));

    /** Flowing cuboid, outwards or inwards */
    public FluidCuboid flow(boolean out) {
      return out ? this.out : in;
    }
  }
}
