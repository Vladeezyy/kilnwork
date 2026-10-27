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
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;

/** {@link KilnworkClientPlatform} on Fabric: Fabric's fluid variant rendering */
public class FabricClientPlatform implements KilnworkClientPlatform {
  private static TextureAtlasSprite missing() {
    return dev.vladeezyy.kilnwork.client.render.FluidCuboidRenderer.getBlockSprite(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
  }

  private static TextureAtlasSprite[] sprites(FluidStack fluid) {
    FluidVariant variant = fluid.isEmpty() ? FluidVariant.of(Fluids.WATER) : FluidAmounts.toVariant(fluid);
    return FluidVariantRendering.getSprites(variant);
  }

  @Override
  public TextureAtlasSprite stillSprite(FluidStack fluid) {
    TextureAtlasSprite[] sprites = sprites(fluid);
    return sprites != null && sprites.length > 0 && sprites[0] != null ? sprites[0] : missing();
  }

  @Override
  public TextureAtlasSprite flowingSprite(FluidStack fluid) {
    TextureAtlasSprite[] sprites = sprites(fluid);
    if (sprites != null && sprites.length > 1 && sprites[1] != null) {
      return sprites[1];
    }
    return stillSprite(fluid);
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
