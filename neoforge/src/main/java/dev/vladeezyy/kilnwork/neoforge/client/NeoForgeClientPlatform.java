package dev.vladeezyy.kilnwork.neoforge.client;

import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.neoforge.fluid.NeoFluidHandlers;
import dev.vladeezyy.kilnwork.platform.KilnworkClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import javax.annotation.Nullable;

/** {@link KilnworkClientPlatform} on NeoForge */
public class NeoForgeClientPlatform implements KilnworkClientPlatform {
  private static TextureAtlasSprite sprite(net.minecraft.resources.Identifier texture) {
    return dev.vladeezyy.kilnwork.client.render.FluidCuboidRenderer.getBlockSprite(texture);
  }

  @Override
  public TextureAtlasSprite stillSprite(FluidStack fluid) {
    return sprite(IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(NeoFluidHandlers.toNeo(fluid)));
  }

  @Override
  public TextureAtlasSprite flowingSprite(FluidStack fluid) {
    return sprite(IClientFluidTypeExtensions.of(fluid.getFluid()).getFlowingTexture(NeoFluidHandlers.toNeo(fluid)));
  }

  @Override
  public int tint(FluidStack fluid, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
    IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
    if (level != null && pos != null) {
      return extensions.getTintColor(fluid.getFluid().defaultFluidState(), level, pos);
    }
    return extensions.getTintColor(NeoFluidHandlers.toNeo(fluid));
  }
}
