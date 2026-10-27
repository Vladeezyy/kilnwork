package dev.vladeezyy.kilnwork.neoforge;

import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.KilnworkRegistries;
import dev.vladeezyy.kilnwork.neoforge.fluid.NeoFluidHandlers;
import dev.vladeezyy.kilnwork.platform.LoaderHooks;
import dev.vladeezyy.kilnwork.platform.Registrar;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry of Kilnwork; mods built on Kilnwork call {@link #register(String, IEventBus)} from their constructor */
@Mod(Kilnwork.MOD_ID)
public class KilnworkNeoForge {
  private static boolean hooksAdded;

  public KilnworkNeoForge(IEventBus bus, ModContainer container) {
    Kilnwork.init();
    KilnworkRegistries.register(bus);
    register(Kilnwork.MOD_ID, bus);
    addLoaderHooks(bus);
  }

  /** Registers the {@link Registrar} entries of the mod on its own mod bus */
  public static void register(String modId, IEventBus bus) {
    bus.addListener(RegisterEvent.class, event -> {
      for (Registrar<?> registrar : Registrar.of(modId)) {
        if (registrar.registryKey().equals(event.getRegistryKey())) {
          registerAll(event, registrar);
        }
      }
    });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <R> void registerAll(RegisterEvent event, Registrar<R> registrar) {
    for (Registrar.Entry<R, ? extends R> entry : registrar.getEntries()) {
      event.register((net.minecraft.resources.ResourceKey) registrar.registryKey(), entry.getId(), () -> Registrar.create(entry));
    }
    Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.get(registrar.registryKey().location());
    if (registry != null) {
      registrar.aliases().forEach(registry::addAlias);
    }
  }

  /** Applies {@link LoaderHooks} of every mod, once, from Kilnwork's own bus */
  private static void addLoaderHooks(IEventBus bus) {
    if (hooksAdded) {
      return;
    }
    hooksAdded = true;
    bus.addListener(KilnworkNeoForge::registerCapabilities);
    bus.addListener(KilnworkNeoForge::registerPayloads);
    NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, event -> LoaderHooks.serverReloadListeners().forEach(listener -> event.addListener(listener.listener())));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void registerCapabilities(RegisterCapabilitiesEvent event) {
    for (LoaderHooks.BlockFluidHandler<?> hook : LoaderHooks.blockFluidHandlers()) {
      registerBlockFluidHandler(event, hook);
    }
    for (LoaderHooks.ItemFluidHandler hook : LoaderHooks.itemFluidHandlers()) {
      event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> NeoFluidHandlers.toNeo(hook.handler().apply(stack)), hook.item().get());
    }
  }

  private static <B extends net.minecraft.world.level.block.entity.BlockEntity> void registerBlockFluidHandler(RegisterCapabilitiesEvent event, LoaderHooks.BlockFluidHandler<B> hook) {
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, hook.type().get(), (be, side) -> NeoFluidHandlers.toNeo(hook.handler().apply(be, side)));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void registerPayloads(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar("1");
    for (LoaderHooks.ClientboundPacket packet : LoaderHooks.clientboundPackets()) {
      registrar.playToClient(packet.type(), packet.codec(), (payload, context) -> context.enqueueWork(() -> packet.handler().accept(payload, (Player) context.player())));
    }
  }
}
