package dev.vladeezyy.kilnwork.neoforge.client;

import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.client.KilnworkClient;
import dev.vladeezyy.kilnwork.client.model.ModelHelpers;
import dev.vladeezyy.kilnwork.client.model.RetexturedModel;
import dev.vladeezyy.kilnwork.client.model.SimpleBlockModel;
import dev.vladeezyy.kilnwork.platform.ClientLoaderHooks;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.common.NeoForge;

/** Client side of Kilnwork on NeoForge: applies {@link ClientLoaderHooks} of every mod and the gauge tooltip */
@SuppressWarnings({"removal", "unchecked", "rawtypes"})
@EventBusSubscriber(modid = Kilnwork.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KilnworkNeoForgeClient {
  private KilnworkNeoForgeClient() {}

  @SubscribeEvent
  static void construct(FMLConstructModEvent event) {
    KilnworkClient.init();
    NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, gui -> KilnworkClient.renderGaugeTooltip(gui.getGuiGraphics()));
  }

  @SubscribeEvent
  static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
    ClientLoaderHooks.reloadListeners().forEach(listener -> event.registerReloadListener(listener.listener()));
    event.registerReloadListener((ResourceManagerReloadListener) manager -> ModelHelpers.clearCache());
  }

  @SubscribeEvent
  static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    for (ClientLoaderHooks.Renderer<?> renderer : ClientLoaderHooks.renderers()) {
      registerRenderer(event, renderer);
    }
  }

  @SubscribeEvent
  static void registerScreens(RegisterMenuScreensEvent event) {
    for (ClientLoaderHooks.ScreenFactory<?, ?> screen : ClientLoaderHooks.screens()) {
      registerScreen(event, screen);
    }
  }

  private static <B extends net.minecraft.world.level.block.entity.BlockEntity> void registerRenderer(EntityRenderersEvent.RegisterRenderers event, ClientLoaderHooks.Renderer<B> renderer) {
    event.registerBlockEntityRenderer((net.minecraft.world.level.block.entity.BlockEntityType<? extends B>) renderer.type().get(), renderer.provider());
  }

  private static <M extends net.minecraft.world.inventory.AbstractContainerMenu, S extends net.minecraft.client.gui.screens.Screen & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void registerScreen(RegisterMenuScreensEvent event, ClientLoaderHooks.ScreenFactory<M, S> screen) {
    event.register((net.minecraft.world.inventory.MenuType<? extends M>) screen.type().get(), screen.constructor());
  }

  @SubscribeEvent
  static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
    event.register(RetexturedModel.ID, RetexturedModel.LOADER);
    event.register(Kilnwork.id("colored_block"), (IGeometryLoader<SimpleBlockModel>) SimpleBlockModel::deserialize);
  }
}
