package dev.vladeezyy.kilnwork.fabric.client;

import dev.vladeezyy.kilnwork.fabric.transfer.FluidAmounts;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.platform.KilnworkClientPlatform;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;

/** {@link KilnworkClientPlatform} on Fabric: Fabric's fluid variant rendering */
public class FabricClientPlatform implements KilnworkClientPlatform {
  /** Since 26.1 the sprites come from the vanilla fluid model of the fluid state */
  private static net.minecraft.client.renderer.block.FluidModel model(FluidStack fluid) {
    net.minecraft.world.level.material.Fluid type = fluid.isEmpty() ? Fluids.WATER : fluid.getFluid();
    return net.minecraft.client.Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(type.defaultFluidState());
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
    if (fluid.isEmpty()) {
      return -1;
    }
    FluidVariant variant = FluidAmounts.toVariant(fluid);
    int color = level != null && pos != null ? FluidVariantRendering.getColor(variant, level, pos) : FluidVariantRendering.getColor(variant);
    return color | 0xFF000000;
  }
}
