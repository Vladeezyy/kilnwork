package dev.vladeezyy.kilnwork.neoforge.client;

import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.client.KilnworkClient;
import dev.vladeezyy.kilnwork.client.model.RetexturedBlockModel;
import dev.vladeezyy.kilnwork.client.model.RetexturedItemModel;
import dev.vladeezyy.kilnwork.client.model.RetexturedModelLoader;
import dev.vladeezyy.kilnwork.client.model.RetexturedQuads;
import dev.vladeezyy.kilnwork.platform.ClientLoaderHooks;
import dev.vladeezyy.kilnwork.retexture.RetexturedBlock;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/** Client side of Kilnwork on NeoForge: applies {@link ClientLoaderHooks} of every mod, the gauge tooltip and the retextured models */
@SuppressWarnings({"unchecked", "rawtypes"})
@EventBusSubscriber(modid = Kilnwork.MOD_ID, value = Dist.CLIENT)
public final class KilnworkNeoForgeClient {
  private KilnworkNeoForgeClient() {}

  @SubscribeEvent
  static void construct(FMLConstructModEvent event) {
    KilnworkClient.init();
    NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, gui -> KilnworkClient.renderGaugeTooltip(gui.getGuiGraphics()));
  }

  @SubscribeEvent
  static void registerReloadListeners(AddClientReloadListenersEvent event) {
    ClientLoaderHooks.reloadListeners().forEach(listener -> event.addListener(listener.id(), listener.listener()));
  }

  @SubscribeEvent
  static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    for (ClientLoaderHooks.Renderer renderer : ClientLoaderHooks.renderers()) {
      event.registerBlockEntityRenderer((net.minecraft.world.level.block.entity.BlockEntityType) renderer.type().get(), renderer.provider());
    }
  }

  @SubscribeEvent
  static void registerScreens(RegisterMenuScreensEvent event) {
    for (ClientLoaderHooks.ScreenFactory screen : ClientLoaderHooks.screens()) {
      event.register((net.minecraft.world.inventory.MenuType) screen.type().get(), screen.constructor());
    }
  }

  @SubscribeEvent
  static void registerLoaders(ModelEvent.RegisterLoaders event) {
    event.register(RetexturedModelLoader.ID, RetexturedModelLoader.INSTANCE);
  }

  @SubscribeEvent
  static void registerItemModels(RegisterItemModelsEvent event) {
    event.register(RetexturedItemModel.ID, RetexturedItemModel.Unbaked.MAP_CODEC);
  }

  @SubscribeEvent
  static void wrapModels(ModelEvent.ModifyBakingResult event) {
    List<Map.Entry<Predicate<Block>, Function<BlockStateModel, BlockStateModel>>> wrappers = new java.util.ArrayList<>(KilnworkClient.modelWrappers());
    wrappers.add(Map.entry(block -> block instanceof RetexturedBlock, RetexturedBlockModel::new));
    event.getBakingResult().blockStateModels().replaceAll((state, model) -> {
      for (Map.Entry<Predicate<Block>, Function<BlockStateModel, BlockStateModel>> wrapper : wrappers) {
        if (wrapper.getKey().test(state.getBlock())) {
          return wrapper.getValue().apply(model);
        }
      }
      return model;
    });
  }

  @SubscribeEvent
  static void bakingCompleted(ModelEvent.BakingCompleted event) {
    RetexturedQuads.bakingCompleted();
  }
}
