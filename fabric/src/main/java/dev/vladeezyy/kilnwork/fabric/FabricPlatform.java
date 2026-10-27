package dev.vladeezyy.kilnwork.fabric;

import dev.vladeezyy.kilnwork.fabric.transfer.FluidAmounts;
import dev.vladeezyy.kilnwork.fabric.transfer.FluidHandlerStorage;
import dev.vladeezyy.kilnwork.fabric.transfer.StorageFluidHandler;
import dev.vladeezyy.kilnwork.fabric.transfer.StorageFluidHandlerItem;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;
import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** {@link KilnworkPlatform} on Fabric: fluids through the transfer API (81 droplets per millibucket) */
public class FabricPlatform implements KilnworkPlatform {
  @Override
  public String loader() {
    return "fabric";
  }

  @Override
  public String modName(String modId) {
    return FabricLoader.getInstance().getModContainer(modId).map(mod -> mod.getMetadata().getName()).orElse(modId);
  }

  @Override
  public boolean isModLoaded(String modId) {
    return FabricLoader.getInstance().isModLoaded(modId);
  }

  @Override
  public Component fluidName(FluidStack fluid) {
    return FluidVariantAttributes.getName(FluidAmounts.toVariant(fluid));
  }

  @Override
  public String fluidDescriptionId(Fluid fluid) {
    // the key FluidVariantAttributes uses for fluids without a handler: the fluid's block, else the registry id
    return fluid.defaultFluidState().createLegacyBlock().getBlock().getDescriptionId();
  }

  @Override
  public int fluidTemperature(FluidStack fluid) {
    return FluidVariantAttributes.getTemperature(FluidAmounts.toVariant(fluid));
  }

  @Nullable
  @Override
  public SoundEvent fluidFillSound(FluidStack fluid) {
    return FluidVariantAttributes.getFillSound(FluidAmounts.toVariant(fluid));
  }

  @Nullable
  @Override
  public SoundEvent fluidEmptySound(FluidStack fluid) {
    return FluidVariantAttributes.getEmptySound(FluidAmounts.toVariant(fluid));
  }

  @Nullable
  @Override
  public IFluidHandler fluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
    Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, side);
    if (storage == null) {
      return null;
    }
    return storage instanceof FluidHandlerStorage own ? own.getHandler() : new StorageFluidHandler(storage);
  }

  @Nullable
  @Override
  public IFluidHandlerItem fluidHandler(ItemStack stack) {
    return StorageFluidHandlerItem.of(stack);
  }

  @Override
  public <M extends AbstractContainerMenu> MenuType<M> menuType(MenuFactory<M> factory) {
    return new ExtendedScreenHandlerType<M, byte[]>((id, inventory, data) -> factory.create(id, inventory, buffer(inventory, data)), ByteBufCodecs.BYTE_ARRAY);
  }

  private static RegistryFriendlyByteBuf buffer(Inventory inventory, byte[] data) {
    return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), inventory.player.registryAccess());
  }

  @Override
  public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) {
    RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
    extraData.accept(buf);
    byte[] data = new byte[buf.readableBytes()];
    buf.readBytes(data);
    player.openMenu(new ExtendedScreenHandlerFactory<byte[]>() {
      @Override
      public byte[] getScreenOpeningData(ServerPlayer opener) {
        return data;
      }

      @Override
      public Component getDisplayName() {
        return provider.getDisplayName();
      }

      @Nullable
      @Override
      public AbstractContainerMenu createMenu(int id, Inventory inventory, Player opener) {
        return provider.createMenu(id, inventory, opener);
      }
    });
  }

  @Override
  public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
    ServerPlayNetworking.send(player, payload);
  }

  @Override
  public void sendToTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
    for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
      ServerPlayNetworking.send(player, payload);
    }
  }

  @Override
  public CreativeModeTab.Builder creativeTab() {
    return FabricItemGroup.builder();
  }
}
