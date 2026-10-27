package dev.vladeezyy.kilnwork.fabric.client;

import dev.vladeezyy.kilnwork.client.KilnworkClient;
import dev.vladeezyy.kilnwork.fabric.KilnworkFabric;
import dev.vladeezyy.kilnwork.platform.ClientLoaderHooks;
import dev.vladeezyy.kilnwork.platform.LoaderHooks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;

/**
 * Client entry of Kilnwork on Fabric. Mods built on Kilnwork call {@link #register()} at the end of their client
 * initializer: it applies the {@link ClientLoaderHooks} and client packet handlers added so far.
 */
public class KilnworkFabricClient implements ClientModInitializer {
  private static int appliedListeners, appliedRenderers, appliedScreens, appliedPackets;

  @Override
  public void onInitializeClient() {
    KilnworkClient.init();
    HudRenderCallback.EVENT.register((graphics, deltaTracker) -> KilnworkClient.renderGaugeTooltip(graphics));
    register();
  }

  public static synchronized void register() {
    List<LoaderHooks.ReloadListener> listeners = ClientLoaderHooks.reloadListeners();
    for (; appliedListeners < listeners.size(); appliedListeners++) {
      LoaderHooks.ReloadListener listener = listeners.get(appliedListeners);
      ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(KilnworkFabric.identifiable(listener.id(), listener.listener()));
    }
    List<ClientLoaderHooks.Renderer<?>> renderers = ClientLoaderHooks.renderers();
    for (; appliedRenderers < renderers.size(); appliedRenderers++) {
      registerRenderer(renderers.get(appliedRenderers));
    }
    List<ClientLoaderHooks.ScreenFactory<?, ?>> screens = ClientLoaderHooks.screens();
    for (; appliedScreens < screens.size(); appliedScreens++) {
      registerScreen(screens.get(appliedScreens));
    }
    List<LoaderHooks.ClientboundPacket<?>> packets = LoaderHooks.clientboundPackets();
    for (; appliedPackets < packets.size(); appliedPackets++) {
      registerReceiver(packets.get(appliedPackets));
    }
  }

  @SuppressWarnings("unchecked")
  private static <B extends BlockEntity> void registerRenderer(ClientLoaderHooks.Renderer<B> renderer) {
    BlockEntityRenderers.register((BlockEntityType<? extends B>) renderer.type().get(), renderer.provider());
  }

  @SuppressWarnings("unchecked")
  private static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreen(ClientLoaderHooks.ScreenFactory<M, S> screen) {
    MenuScreens.register((MenuType<? extends M>) screen.type().get(), screen.constructor());
  }

  private static <P extends CustomPacketPayload> void registerReceiver(LoaderHooks.ClientboundPacket<P> packet) {
    ClientPlayNetworking.registerGlobalReceiver(packet.type(), (payload, context) -> context.client().execute(() -> packet.handler().accept(payload, context.player())));
  }
}
