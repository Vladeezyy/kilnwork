package dev.vladeezyy.kilnwork;

import dev.vladeezyy.kilnwork.fluid.ItemFluidTransfers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kilnwork: a small library for Vladeezyy's ports, built from the parts of Mantle (SlimeKnights, MIT) that Ceramics needs.
 */
@Mod(Kilnwork.MOD_ID)
public class Kilnwork {
  public static final String MOD_ID = "kilnwork";
  public static final Logger LOG = LoggerFactory.getLogger("Kilnwork");

  public Kilnwork(IEventBus bus, ModContainer container) {
    NeoForge.EVENT_BUS.addListener(Kilnwork::addReloadListeners);
  }

  private static void addReloadListeners(AddServerReloadListenersEvent event) {
    event.addListener(id("fluid_transfers"), ItemFluidTransfers.INSTANCE);
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
