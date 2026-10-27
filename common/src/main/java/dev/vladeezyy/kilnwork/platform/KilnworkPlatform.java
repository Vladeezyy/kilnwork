package dev.vladeezyy.kilnwork.platform;

import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * What Kilnwork and the mods built on it need from the mod loader, implemented once per loader (found with
 * {@link ServiceLoader}). Things registered at a fixed time (fluid handlers, packets, reload listeners, renderers) are
 * collected in {@link LoaderHooks} instead and picked up by the loader.
 */
public interface KilnworkPlatform {
  KilnworkPlatform INSTANCE = ServiceLoader.load(KilnworkPlatform.class, KilnworkPlatform.class.getClassLoader()).findFirst()
    .orElseThrow(() -> new IllegalStateException("No Kilnwork platform: the Kilnwork jar of this mod loader is missing"));

  static KilnworkPlatform get() {
    return INSTANCE;
  }

  /** Name of the loader, "neoforge" or "fabric" */
  String loader();

  /** Display name of the mod with the given id, or the id */
  String modName(String modId);

  /** True if a mod with the given id is loaded */
  boolean isModLoaded(String modId);

  /* Fluids */

  /** Name of the fluid stack shown to players */
  Component fluidName(FluidStack fluid);

  /** Translation key of the fluid */
  String fluidDescriptionId(Fluid fluid);

  /** Temperature of the fluid in Kelvin (water 300, lava 1300) */
  int fluidTemperature(FluidStack fluid);

  /** Light level the fluid gives off (lava 15) */
  int fluidLightLevel(FluidStack fluid);

  /** Sound of filling a bucket with the fluid, null for the default */
  @Nullable
  SoundEvent fluidFillSound(FluidStack fluid);

  /** Sound of emptying a bucket of the fluid, null for the default */
  @Nullable
  SoundEvent fluidEmptySound(FluidStack fluid);

  /** Fluid handler of the block at the position seen from the given side, also of blocks of other mods */
  @Nullable
  IFluidHandler fluidHandler(Level level, BlockPos pos, @Nullable Direction side);

  /**
   * Fluid handler of an item stack, also of items of other mods (buckets of any fluid). The handler works on its own
   * copy of the stack; {@link IFluidHandlerItem#getContainer()} gives the result.
   */
  @Nullable
  IFluidHandlerItem fluidHandler(ItemStack stack);

  /**
   * Cached lookup of the fluid handler at a position, for block entities that check a neighbor often (NeoForge
   * BlockCapabilityCache, Fabric BlockApiCache). {@code isValid} is false once the caller is removed.
   */
  FluidHandlerCache fluidHandlerCache(ServerLevel level, BlockPos pos, @Nullable Direction side, java.util.function.BooleanSupplier isValid);

  /** Tells cached lookups that the fluid handlers of the block at the position changed (NeoForge invalidateCapabilities) */
  void invalidateFluidHandlers(Level level, BlockPos pos);

  @FunctionalInterface
  interface FluidHandlerCache {
    @Nullable
    IFluidHandler get();
  }

  /* Menus and networking */

  /** Menu type whose client side reads extra data sent with {@link #openMenu} */
  <M extends AbstractContainerMenu> MenuType<M> menuType(MenuFactory<M> factory);

  /** Opens a menu, writing the extra data for the client */
  void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData);

  /** Sends a packet registered in {@link LoaderHooks#clientboundPacket} to one player */
  void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

  /** Sends a packet to every player tracking the chunk at the position */
  void sendToTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload);

  /** Builder of a creative tab (NeoForge and Fabric take no row and column) */
  CreativeModeTab.Builder creativeTab();

  @FunctionalInterface
  interface MenuFactory<M extends AbstractContainerMenu> {
    M create(int id, Inventory inventory, RegistryFriendlyByteBuf extraData);
  }
}
