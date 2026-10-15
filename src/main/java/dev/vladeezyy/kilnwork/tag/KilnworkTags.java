package dev.vladeezyy.kilnwork.tag;

import dev.vladeezyy.kilnwork.Kilnwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/** Tags of Kilnwork */
public final class KilnworkTags {
  private KilnworkTags() {}

  public static final class Blocks {
    private Blocks() {}
    /** Blocks that show a fluid tooltip when looked at: {@link #ATTACHED_GAUGES} and {@link #GAUGE_TANKS} */
    public static final TagKey<Block> GAUGES = tag("gauges");
    /** Gauges showing the fluid of the block they are attached to, must have {@code facing} */
    public static final TagKey<Block> ATTACHED_GAUGES = tag("gauges/attached");
    /** Tanks showing their own fluid when looked at */
    public static final TagKey<Block> GAUGE_TANKS = tag("gauges/tank");
    /** Blocks a gauge shows nothing for */
    public static final TagKey<Block> GAUGE_BLACKLIST = tag("gauge_blacklist");
    /** Blocks that do not sync their full amount, a gauge shows just the fluid */
    public static final TagKey<Block> HIDES_GAUGE_AMOUNT = tag("hides_gauge_amount");

    private static TagKey<Block> tag(String name) {
      return TagKey.create(Registries.BLOCK, Kilnwork.id(name));
    }
  }

  public static final class Fluids {
    private Fluids() {}
    /** Powder snow as a fluid, if a mod adds one */
    public static final TagKey<Fluid> POWDER_SNOW = common("powdered_snow");
    /** Potions as fluids, if a mod adds them */
    public static final TagKey<Fluid> POTION = common("potion");

    private static TagKey<Fluid> common(String name) {
      return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", name));
    }
  }
}
