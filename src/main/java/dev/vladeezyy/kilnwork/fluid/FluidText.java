package dev.vladeezyy.kilnwork.fluid;

import dev.vladeezyy.kilnwork.Kilnwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.text.NumberFormat;
import java.util.Locale;

/** Text for fluid amounts, used by gauges and fluid transfers */
public final class FluidText {
  private FluidText() {}

  private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance(Locale.US);

  /** Formats a number with thousands separators */
  public static String number(int value) {
    return FORMAT.format(value);
  }

  /** "Capacity: 4,000 mB" */
  public static MutableComponent capacity(int capacity) {
    return Kilnwork.component("gui", "fluid.capacity", number(capacity));
  }

  /** "2,000 / 4,000 mB Water" */
  public static MutableComponent contents(FluidStack fluid, int capacity) {
    return Kilnwork.component("gui", "fluid.contents", Kilnwork.component("gui", "fluid.format", number(fluid.getAmount()), number(capacity), fluid.getHoverName()));
  }

  /** Amount in mB, and in buckets when it is a whole number of buckets */
  public static MutableComponent amount(int amount) {
    if (amount >= FluidType.BUCKET_VOLUME && amount % FluidType.BUCKET_VOLUME == 0) {
      return Kilnwork.component("gui", "fluid.buckets", number(amount / FluidType.BUCKET_VOLUME), number(amount));
    }
    return Kilnwork.component("gui", "fluid.millibuckets", number(amount));
  }

  /** Name of the mod adding the fluid, blue and italic as in vanilla tooltips */
  public static Component modName(FluidStack fluid) {
    String namespace = fluid.getFluidHolder().unwrapKey().map(key -> key.location().getNamespace()).orElse("minecraft");
    String name = net.neoforged.fml.ModList.get().getModContainerById(namespace).map(mod -> mod.getModInfo().getDisplayName()).orElse(namespace);
    return Component.literal(name).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
  }
}
