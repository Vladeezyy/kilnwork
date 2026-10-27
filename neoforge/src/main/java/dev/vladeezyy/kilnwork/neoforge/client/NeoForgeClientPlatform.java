package dev.vladeezyy.kilnwork.neoforge.client;

import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.neoforge.fluid.NeoFluidHandlers;
import dev.vladeezyy.kilnwork.platform.KilnworkClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

import javax.annotation.Nullable;

/** {@link KilnworkClientPlatform} on NeoForge: since 26.1 sprites and tint come from the fluid model of the fluid state */
public class NeoForgeClientPlatform implements KilnworkClientPlatform {
  private static FluidModel model(FluidStack fluid) {
    return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
  }

  @Override
  public TextureAtlasSprite stillSprite(FluidStack fluid) {
    return model(fluid).stillMaterial().sprite();
  }

  @Override
  public TextureAtlasSprite flowingSprite(FluidStack fluid) {
    return model(fluid).flowingMaterial().sprite();
  }

  @Override
  public int tint(FluidStack fluid, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
    FluidTintSource tint = model(fluid).fluidTintSource();
    if (tint == null) {
      return -1;
    }
    if (level != null && pos != null) {
      return tint.colorInWorld(fluid.getFluid().defaultFluidState(), level.getBlockState(pos), level, pos);
    }
    return tint.colorAsStack(NeoFluidHandlers.toNeo(fluid));
  }
}
