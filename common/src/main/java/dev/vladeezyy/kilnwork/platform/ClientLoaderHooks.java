package dev.vladeezyy.kilnwork.platform;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Client registrations the loader applies at its own time; call these from the mod's client initializer */
public final class ClientLoaderHooks {
  private ClientLoaderHooks() {}

  public record Renderer<B extends BlockEntity>(Supplier<? extends BlockEntityType<? extends B>> type, BlockEntityRendererProvider<B> provider) {}
  public record ScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>>(Supplier<? extends MenuType<? extends M>> type, MenuScreens.ScreenConstructor<M, S> constructor) {}

  private static final List<LoaderHooks.ReloadListener> RELOAD_LISTENERS = new ArrayList<>();
  private static final List<Renderer<?>> RENDERERS = new ArrayList<>();
  private static final List<ScreenFactory<?, ?>> SCREENS = new ArrayList<>();

  /** Resource reload listener (client resource packs) */
  public static synchronized void reloadListener(ResourceLocation id, PreparableReloadListener listener) {
    RELOAD_LISTENERS.add(new LoaderHooks.ReloadListener(id, listener));
  }

  public static synchronized <B extends BlockEntity> void blockEntityRenderer(Supplier<? extends BlockEntityType<? extends B>> type, BlockEntityRendererProvider<B> provider) {
    RENDERERS.add(new Renderer<>(type, provider));
  }

  public static synchronized <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menuScreen(Supplier<? extends MenuType<? extends M>> type, MenuScreens.ScreenConstructor<M, S> constructor) {
    SCREENS.add(new ScreenFactory<>(type, constructor));
  }

  public static synchronized List<LoaderHooks.ReloadListener> reloadListeners() {
    return Collections.unmodifiableList(new ArrayList<>(RELOAD_LISTENERS));
  }

  public static synchronized List<Renderer<?>> renderers() {
    return Collections.unmodifiableList(new ArrayList<>(RENDERERS));
  }

  public static synchronized List<ScreenFactory<?, ?>> screens() {
    return Collections.unmodifiableList(new ArrayList<>(SCREENS));
  }
}
