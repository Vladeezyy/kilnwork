package dev.vladeezyy.kilnwork.fluid;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.tag.TagPreference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Fluid transfers for items without a fluid handler, such as bowls of stew or sponges, from data packs:
 * {@code data/<namespace>/kilnwork/fluid_transfer/<name>.json}
 * <pre>{"type": "fill_item", "input": {"item": "minecraft:sponge"}, "fluid": {"tag": "c:water", "amount": 250}, "result": "minecraft:wet_sponge"}</pre>
 * {@code fill_item} takes fluid out of the tank into the item, {@code empty_item} puts the fluid of the item into the tank.
 * The fluid is either {@code "fluid": "<id>"} or {@code "tag": "<fluid tag>"}; transfers whose fluid does not exist are skipped.
 */
public class ItemFluidTransfers extends SimpleJsonResourceReloadListener {
  public static final ItemFluidTransfers INSTANCE = new ItemFluidTransfers();

  private List<Transfer> transfers = List.of();

  private ItemFluidTransfers() {
    super(new com.google.gson.Gson(), "kilnwork/fluid_transfer");
  }

  @Override
  protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
    List<Transfer> list = new ArrayList<>();
    jsons.forEach((id, json) -> Transfer.CODEC.parse(JsonOps.INSTANCE, json)
      .resultOrPartial(error -> Kilnwork.LOG.error("Failed to parse fluid transfer {}: {}", id, error))
      .ifPresent(list::add));
    transfers = List.copyOf(list);
  }

  /** True if some transfer may apply to the stack */
  public boolean mayHaveTransfer(ItemStack stack) {
    for (Transfer transfer : transfers) {
      if (transfer.input.test(stack)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Runs the first matching transfer
   * @param stack  Held stack, not modified
   * @param tank   Tank to fill or drain
   * @return  Result, or null if nothing matched
   */
  @Nullable
  public Result transfer(ItemStack stack, IFluidHandler tank) {
    for (Transfer transfer : transfers) {
      if (transfer.input.test(stack)) {
        Result result = transfer.run(tank);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  /**
   * Result of a transfer
   * @param stack   New item stack
   * @param fluid   Fluid moved
   * @param filled  True if the item was filled (tank drained)
   */
  public record Result(ItemStack stack, FluidStack fluid, boolean filled) {}

  private enum Type implements StringRepresentable {
    FILL_ITEM, EMPTY_ITEM;
    static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

    @Override
    public String getSerializedName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** Fluid of a transfer: a fluid id or a fluid tag, and an amount */
  private record FluidSource(Optional<Fluid> fluid, Optional<TagKey<Fluid>> tag, int amount) {
    static final Codec<FluidSource> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("fluid").forGetter(FluidSource::fluid),
      TagKey.codec(Registries.FLUID).optionalFieldOf("tag").forGetter(FluidSource::tag),
      Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(FluidSource::amount)
    ).apply(inst, FluidSource::new));

    boolean matches(FluidStack stack) {
      return fluid.map(stack::is).orElse(false) || tag.map(stack::is).orElse(false);
    }

    /** Fluid to put into a tank */
    Optional<Fluid> get() {
      return fluid.isPresent() ? fluid : tag.flatMap(TagPreference::getPreference);
    }
  }

  private record Transfer(Type type, Ingredient input, FluidSource fluid, Item result) {
    static final Codec<Transfer> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      Type.CODEC.fieldOf("type").forGetter(Transfer::type),
      Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(Transfer::input),
      FluidSource.CODEC.fieldOf("fluid").forGetter(Transfer::fluid),
      BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(Transfer::result)
    ).apply(inst, Transfer::new));

    @Nullable
    Result run(IFluidHandler tank) {
      if (type == Type.FILL_ITEM) {
        FluidStack inTank = tank.drain(fluid.amount(), FluidAction.SIMULATE);
        if (inTank.getAmount() == fluid.amount() && fluid.matches(inTank)) {
          return new Result(new ItemStack(result), tank.drain(inTank, FluidAction.EXECUTE), true);
        }
      } else {
        Optional<Fluid> source = fluid.get();
        if (source.isPresent()) {
          FluidStack stack = new FluidStack(source.get(), fluid.amount());
          if (tank.fill(stack, FluidAction.SIMULATE) == fluid.amount()) {
            tank.fill(stack.copy(), FluidAction.EXECUTE);
            return new Result(new ItemStack(result), stack, false);
          }
        }
      }
      return null;
    }
  }
}
