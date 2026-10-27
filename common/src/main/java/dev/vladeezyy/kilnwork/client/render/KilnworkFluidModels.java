package dev.vladeezyy.kilnwork.client.render;

import dev.vladeezyy.kilnwork.data.BlockDataLoader;
import dev.vladeezyy.kilnwork.data.BlockStateDataLoader;

import java.util.List;

/** Client data with fluid shapes for block entity renderers, loaded with resource packs */
public final class KilnworkFluidModels {
  private KilnworkFluidModels() {}

  /** Fluid cuboids a block entity draws, per block state: {@code kilnwork/block_fluids} */
  public static final BlockStateDataLoader<List<FluidCuboid>> BLOCK_FLUIDS = BlockStateDataLoader.of("block fluids", "kilnwork/block_fluids", FluidCuboid.LIST_CODEC);
  /** Fluid under a faucet or channel: {@code kilnwork/faucet_fluid} */
  public static final FaucetFluid.Loader FAUCET_FLUIDS = new FaucetFluid.Loader();
  /** Fluid shapes of channels: {@code kilnwork/channel_fluids} */
  public static final BlockDataLoader<ChannelFluids> CHANNEL_FLUIDS = new BlockDataLoader<>("channel fluids", "kilnwork/channel_fluids", ChannelFluids.CODEC);
}
