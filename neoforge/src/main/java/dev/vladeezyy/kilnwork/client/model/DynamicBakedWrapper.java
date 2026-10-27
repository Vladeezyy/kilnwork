package dev.vladeezyy.kilnwork.client.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Baked model wrapper whose quads depend on model data only. Based on Mantle's DynamicBakedWrapper (MIT, SlimeKnights).
 */
public abstract class DynamicBakedWrapper<T extends BakedModel> extends BakedModelWrapper<T> {
  protected DynamicBakedWrapper(T originalModel) {
    super(originalModel);
  }

  @Override
  @Deprecated
  public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
    return getQuads(state, side, rand, ModelData.EMPTY, null);
  }

  @Override
  public abstract List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType);
}
