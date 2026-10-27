package dev.vladeezyy.kilnwork.platform;

import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registrations the loader applies at its own time: call these from the mod's constructor or initializer, the
 * NeoForge or Fabric part of Kilnwork picks them up (capability event, payload registration, reload listeners).
 */
public final class LoaderHooks {
  private LoaderHooks() {}

  public record BlockFluidHandler<B extends BlockEntity>(Supplier<? extends BlockEntityType<B>> type, BiFunction<B, Direction, IFluidHandler> handler) {}
  public record ItemFluidHandler(Supplier<? extends Item> item, Function<net.minecraft.world.item.ItemStack, IFluidHandlerItem> handler) {}
  public record ClientboundPacket<P extends CustomPacketPayload>(CustomPacketPayload.Type<P> type, StreamCodec<? super RegistryFriendlyByteBuf, P> codec, BiConsumer<P, Player> handler) {}
  public record ReloadListener(ResourceLocation id, PreparableReloadListener listener) {}

  private static final List<BlockFluidHandler<?>> BLOCK_FLUID_HANDLERS = new ArrayList<>();
  private static final List<ItemFluidHandler> ITEM_FLUID_HANDLERS = new ArrayList<>();
  private static final List<ClientboundPacket<?>> CLIENTBOUND_PACKETS = new ArrayList<>();
  private static final List<ReloadListener> SERVER_RELOAD_LISTENERS = new ArrayList<>();

  /** Fluid handler of a block entity type of this mod */
  public static synchronized <B extends BlockEntity> void blockFluidHandler(Supplier<? extends BlockEntityType<B>> type, BiFunction<B, Direction, IFluidHandler> handler) {
    BLOCK_FLUID_HANDLERS.add(new BlockFluidHandler<>(type, handler));
  }

  /** Fluid handler of an item of this mod; the handler gets its own copy of the stack */
  public static synchronized void itemFluidHandler(Supplier<? extends Item> item, Function<net.minecraft.world.item.ItemStack, IFluidHandlerItem> handler) {
    ITEM_FLUID_HANDLERS.add(new ItemFluidHandler(item, handler));
  }

  /** A packet from the server to the client; the handler runs on the client thread with the client player */
  public static synchronized <P extends CustomPacketPayload> void clientboundPacket(CustomPacketPayload.Type<P> type, StreamCodec<? super RegistryFriendlyByteBuf, P> codec, BiConsumer<P, Player> handler) {
    CLIENTBOUND_PACKETS.add(new ClientboundPacket<>(type, codec, handler));
  }

  /** Data reload listener (server data packs) */
  public static synchronized void serverReloadListener(ResourceLocation id, PreparableReloadListener listener) {
    SERVER_RELOAD_LISTENERS.add(new ReloadListener(id, listener));
  }

  public static synchronized List<BlockFluidHandler<?>> blockFluidHandlers() {
    return Collections.unmodifiableList(new ArrayList<>(BLOCK_FLUID_HANDLERS));
  }

  public static synchronized List<ItemFluidHandler> itemFluidHandlers() {
    return Collections.unmodifiableList(new ArrayList<>(ITEM_FLUID_HANDLERS));
  }

  public static synchronized List<ClientboundPacket<?>> clientboundPackets() {
    return Collections.unmodifiableList(new ArrayList<>(CLIENTBOUND_PACKETS));
  }

  public static synchronized List<ReloadListener> serverReloadListeners() {
    return Collections.unmodifiableList(new ArrayList<>(SERVER_RELOAD_LISTENERS));
  }
}
