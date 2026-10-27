package dev.vladeezyy.kilnwork.platform;

import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;

import javax.annotation.Nullable;
import java.util.ServiceLoader;

/** Client side of {@link KilnworkPlatform}: how fluids look */
public interface KilnworkClientPlatform {
  KilnworkClientPlatform INSTANCE = ServiceLoader.load(KilnworkClientPlatform.class, KilnworkClientPlatform.class.getClassLoader()).findFirst()
    .orElseThrow(() -> new IllegalStateException("No Kilnwork client platform: the Kilnwork jar of this mod loader is missing"));

  static KilnworkClientPlatform get() {
    return INSTANCE;
  }

  /** Texture of the still fluid, on the block atlas */
  TextureAtlasSprite stillSprite(FluidStack fluid);

  /** Texture of the flowing fluid, on the block atlas */
  TextureAtlasSprite flowingSprite(FluidStack fluid);

  /** Tint of the fluid as ARGB; with a level and position for biome colored fluids (water) */
  int tint(FluidStack fluid, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos);
}
