package dev.vladeezyy.kilnwork;

import dev.vladeezyy.kilnwork.fluid.ItemFluidTransfers;
import dev.vladeezyy.kilnwork.platform.LoaderHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kilnwork: a small library for Vladeezyy's ports, built from the parts of Mantle (SlimeKnights, MIT) that Ceramics and
 * Inspirations need. The loader parts call {@link #init()} when the mod starts.
 */
public final class Kilnwork {
  public static final String MOD_ID = "kilnwork";
  public static final Logger LOG = LoggerFactory.getLogger("Kilnwork");

  private Kilnwork() {}

  /** Registrations of Kilnwork itself, the same on every loader */
  public static void init() {
    LoaderHooks.serverReloadListener(id("fluid_transfers"), ItemFluidTransfers.INSTANCE::withRegistries);
  }

  /** Location in the Kilnwork namespace */
  public static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(MOD_ID, path);
  }

  /** Translation key in the Kilnwork namespace, such as {@code gui.kilnwork.fluid.contents} */
  public static String lang(String group, String name) {
    return group + '.' + MOD_ID + '.' + name;
  }

  /** Translatable component in the Kilnwork namespace */
  public static MutableComponent component(String group, String name, Object... args) {
    return Component.translatable(lang(group, name), args);
  }
}
