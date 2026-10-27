package dev.vladeezyy.kilnwork.fabric;

import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.fabric.transfer.FluidHandlerItemStorage;
import dev.vladeezyy.kilnwork.fabric.transfer.FluidHandlerStorage;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.platform.LoaderHooks;
import dev.vladeezyy.kilnwork.platform.Registrar;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fabric entry of Kilnwork. Mods built on Kilnwork call {@link #register(String)} at the end of their initializer:
 * it registers their {@link Registrar} entries and applies the {@link LoaderHooks} added so far.
 */
public class KilnworkFabric implements ModInitializer {
  private static int appliedBlockFluidHandlers, appliedItemFluidHandlers, appliedPackets, appliedReloadListeners;
  private static final Set<String> REGISTERED = new HashSet<>();

  @Override
  public void onInitialize() {
    Kilnwork.init();
    register(Kilnwork.MOD_ID);
    net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> LoaderHooks.fireTagsLoaded());
  }

  /** Registers the entries of the mod (in the order of the registries, as NeoForge does) and applies the loader hooks */
  public static synchronized void register(String modId) {
    if (REGISTERED.add(modId)) {
      List<Registrar<?>> registrars = new ArrayList<>(Registrar.of(modId));
      // NeoForge's order: attributes and data components first (items need them), then the registries
      // in creation order (BuiltInRegistries.REGISTRY iterates in it; the key set of the root registry is not ordered)
      List<Identifier> ordered = new ArrayList<>(List.of(net.minecraft.core.registries.Registries.ATTRIBUTE.identifier(),
        net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE.identifier()));
      for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
        if (!ordered.contains(registry.key().identifier())) {
          ordered.add(registry.key().identifier());
        }
      }
      registrars.sort(Comparator.comparingInt(r -> {
        int index = ordered.indexOf(r.registryKey().identifier());
        return index < 0 ? Integer.MAX_VALUE : index;
      }));
      for (Registrar<?> registrar : registrars) {
        registerAll(registrar);
      }
    }
    applyHooks();
  }

  @SuppressWarnings("unchecked")
  private static <R> void registerAll(Registrar<R> registrar) {
    Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(registrar.registryKey().identifier());
    if (registry == null) {
      throw new IllegalStateException("Unknown registry " + registrar.registryKey().identifier());
    }
    for (Registrar.Entry<R, ? extends R> entry : registrar.getEntries()) {
      Registry.register(registry, (ResourceKey<R>) entry.getKey(), Registrar.create(entry));
    }
    registrar.aliases().forEach((from, to) -> RegistryAliases.add(registrar.registryKey(), from, to));
  }

  private static void applyHooks() {
    List<LoaderHooks.BlockFluidHandler<?>> blocks = LoaderHooks.blockFluidHandlers();
    for (; appliedBlockFluidHandlers < blocks.size(); appliedBlockFluidHandlers++) {
      registerBlockFluidHandler(blocks.get(appliedBlockFluidHandlers));
    }
    List<LoaderHooks.ItemFluidHandler> items = LoaderHooks.itemFluidHandlers();
    for (; appliedItemFluidHandlers < items.size(); appliedItemFluidHandlers++) {
      LoaderHooks.ItemFluidHandler hook = items.get(appliedItemFluidHandlers);
      FluidStorage.ITEM.registerForItems((stack, context) -> new FluidHandlerItemStorage(context, hook.handler()), hook.item().get());
    }
    List<LoaderHooks.ClientboundPacket<?>> packets = LoaderHooks.clientboundPackets();
    for (; appliedPackets < packets.size(); appliedPackets++) {
      registerPacketType(packets.get(appliedPackets));
    }
    List<LoaderHooks.ServerReloadListener> listeners = LoaderHooks.serverReloadListeners();
    for (; appliedReloadListeners < listeners.size(); appliedReloadListeners++) {
      LoaderHooks.ServerReloadListener listener = listeners.get(appliedReloadListeners);
      net.fabricmc.fabric.api.resource.v1.DataResourceLoader.get().registerReloadListener(listener.id(), listener.factory());
    }
  }

  private static <B extends BlockEntity> void registerBlockFluidHandler(LoaderHooks.BlockFluidHandler<B> hook) {
    FluidStorage.SIDED.registerForBlockEntity((be, side) -> {
      IFluidHandler handler = hook.handler().apply(be, side);
      return handler == null ? null : new FluidHandlerStorage(handler);
    }, hook.type().get());
  }

  @SuppressWarnings("unchecked")
  private static <P extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void registerPacketType(LoaderHooks.ClientboundPacket<P> packet) {
    PayloadTypeRegistry.clientboundPlay().register(packet.type(), (net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, P>) packet.codec());
  }

}
