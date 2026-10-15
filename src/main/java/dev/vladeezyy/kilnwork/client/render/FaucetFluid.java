package dev.vladeezyy.kilnwork.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.data.BlockStateDataLoader;
import dev.vladeezyy.kilnwork.data.JsonParents;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/**
 * Fluid drawn in the block below a pouring faucet or channel.
 * JSON per block state in {@code assets/<namespace>/kilnwork/faucet_fluid/}, with a default in
 * {@code assets/kilnwork/kilnwork/faucet_fluid/_default.json}. Keys: {@code side} and {@code center} (cuboids for a side
 * faucet and one pouring straight down), or {@code bottom} (a number) to move the bottom of the default cuboids,
 * {@code continue} (true if the stream goes on into the block below).
 * @param side         Cuboids under a faucet facing sideways
 * @param center       Cuboids under a faucet facing down
 * @param isContinued  If true, the fluid also flows into the block below
 */
public record FaucetFluid(List<FluidCuboid> side, List<FluidCuboid> center, boolean isContinued) {
  public static final FaucetFluid EMPTY = new FaucetFluid(List.of(), List.of(), false);
  private static final ResourceLocation DEFAULT = Kilnwork.id("_default");

  /** Cuboids for the given faucet direction */
  public List<FluidCuboid> getFluids(Direction direction) {
    return direction.getAxis() == Direction.Axis.Y ? center : side;
  }

  /** Loader with the default */
  public static final class Loader extends BlockStateDataLoader<FaucetFluid> {
    private FaucetFluid defaultValue = EMPTY;

    public Loader() {
      super("faucet fluids", "kilnwork/faucet_fluid");
    }

    @Override
    protected void prepare(Map<ResourceLocation, JsonElement> jsons) {
      defaultValue = EMPTY;
      JsonElement json = jsons.get(DEFAULT);
      if (json == null) {
        Kilnwork.LOG.warn("No default faucet fluid, is the Kilnwork resource pack disabled?");
      } else {
        defaultValue = read(GsonHelper.convertToJsonObject(json, DEFAULT.toString()), EMPTY);
      }
    }

    @Override
    protected FaucetFluid parse(Map<ResourceLocation, JsonElement> jsons, ResourceLocation id, JsonElement element) {
      JsonObject json = JsonParents.resolve(jsons, id, GsonHelper.convertToJsonObject(element, id.toString())).json();
      return read(json, defaultValue);
    }

    private static FaucetFluid read(JsonObject json, FaucetFluid fallback) {
      return new FaucetFluid(cuboids(json, "side", fallback.side), cuboids(json, "center", fallback.center), GsonHelper.getAsBoolean(json, "continue", false));
    }

    private static List<FluidCuboid> cuboids(JsonObject json, String key, List<FluidCuboid> fallback) {
      JsonElement element = json.has(key) ? json.get(key) : json.has("bottom") && json.get("bottom").isJsonPrimitive() ? json.get("bottom") : null;
      if (element == null) {
        return fallback;
      }
      // a number moves the bottom of the default cuboids
      if (element.isJsonPrimitive()) {
        float bottom = element.getAsFloat();
        return fallback.stream().map(cuboid -> cuboid.withBottom(bottom)).toList();
      }
      return FluidCuboid.LIST_CODEC.parse(JsonOps.INSTANCE, element).getOrThrow();
    }

    @Override
    public FaucetFluid get(BlockState state) {
      return get(state, defaultValue);
    }
  }
}
