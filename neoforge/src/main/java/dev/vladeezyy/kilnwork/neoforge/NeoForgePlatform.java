package dev.vladeezyy.kilnwork.neoforge;

import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;
import dev.vladeezyy.kilnwork.neoforge.fluid.NeoFluidHandlers;
import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** {@link KilnworkPlatform} on NeoForge */
public class NeoForgePlatform implements KilnworkPlatform {
  @Override
  public String loader() {
    return "neoforge";
  }

  @Override
  public String modName(String modId) {
    return ModList.get().getModContainerById(modId).map(mod -> mod.getModInfo().getDisplayName()).orElse(modId);
  }

  @Override
  public boolean isModLoaded(String modId) {
    return ModList.get().isLoaded(modId);
  }

  @Override
  public Component fluidName(FluidStack fluid) {
    return NeoFluidHandlers.toNeo(fluid).getHoverName();
  }

  @Override
  public String fluidDescriptionId(Fluid fluid) {
    return fluid.getFluidType().getDescriptionId();
  }

  @Override
  public int fluidTemperature(FluidStack fluid) {
    return fluid.getFluid().getFluidType().getTemperature(NeoFluidHandlers.toNeo(fluid));
  }

  @Override
  public int fluidLightLevel(FluidStack fluid) {
    return fluid.getFluid().getFluidType().getLightLevel(NeoFluidHandlers.toNeo(fluid));
  }

  @Nullable
  @Override
  public SoundEvent fluidFillSound(FluidStack fluid) {
    return fluid.getFluid().getFluidType().getSound(NeoFluidHandlers.toNeo(fluid), SoundActions.BUCKET_FILL);
  }

  @Nullable
  @Override
  public SoundEvent fluidEmptySound(FluidStack fluid) {
    return fluid.getFluid().getFluidType().getSound(NeoFluidHandlers.toNeo(fluid), SoundActions.BUCKET_EMPTY);
  }

  @Nullable
  @Override
  public IFluidHandler fluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
    return NeoFluidHandlers.fromNeo(level.getCapability(Capabilities.Fluid.BLOCK, pos, side));
  }

  @Nullable
  @Override
  public IFluidHandlerItem fluidHandler(ItemStack stack) {
    return NeoFluidHandlers.itemFromNeo(stack);
  }

  @Override
  public void invalidateFluidHandlers(Level level, BlockPos pos) {
    level.invalidateCapabilities(pos);
  }

  @Override
  public FluidHandlerCache fluidHandlerCache(ServerLevel level, BlockPos pos, @Nullable Direction side, java.util.function.BooleanSupplier isValid) {
    BlockCapabilityCache<net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource>, Direction> cache = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, pos, side, isValid, () -> {});
    return new FluidHandlerCache() {
      private net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> last;
      private IFluidHandler wrapped;

      @Nullable
      @Override
      public IFluidHandler get() {
        net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> handler = cache.getCapability();
        if (handler != last) {
          last = handler;
          wrapped = NeoFluidHandlers.fromNeo(handler);
        }
        return wrapped;
      }
    };
  }

  @Override
  public <M extends AbstractContainerMenu> MenuType<M> menuType(MenuFactory<M> factory) {
    return IMenuTypeExtension.create(factory::create);
  }

  @Override
  public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) {
    player.openMenu(provider, extraData);
  }

  @Override
  public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
    PacketDistributor.sendToPlayer(player, payload);
  }

  @Override
  public void sendToTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
    PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), payload);
  }

  @Override
  public CreativeModeTab.Builder creativeTab() {
    return CreativeModeTab.builder();
  }
}
