package dev.vladeezyy.kilnwork.client.model;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;

import javax.annotation.Nullable;
import java.util.Map;

/** Baking context with extra or replaced textures, everything else from the wrapped context */
public class ExtraTextureContext implements IGeometryBakingContext {
  private final IGeometryBakingContext base;
  private final Map<String, Material> textures;

  public ExtraTextureContext(IGeometryBakingContext base, Map<String, Material> textures) {
    this.base = base;
    this.textures = textures;
  }

  @Override
  public Material getMaterial(String name) {
    Material material = textures.get(name);
    return material != null ? material : base.getMaterial(name);
  }

  @Override
  public boolean hasMaterial(String name) {
    return textures.containsKey(name) || base.hasMaterial(name);
  }

  @Override
  public String getModelName() {
    return base.getModelName();
  }

  @Override
  public boolean isGui3d() {
    return base.isGui3d();
  }

  @Override
  public boolean useBlockLight() {
    return base.useBlockLight();
  }

  @Override
  public boolean useAmbientOcclusion() {
    return base.useAmbientOcclusion();
  }

  @Override
  public ItemTransforms getTransforms() {
    return base.getTransforms();
  }

  @Override
  public Transformation getRootTransform() {
    return base.getRootTransform();
  }

  @Override
  @Nullable
  public ResourceLocation getRenderTypeHint() {
    return base.getRenderTypeHint();
  }

  @Override
  public boolean isComponentVisible(String component, boolean fallback) {
    return base.isComponentVisible(component, fallback);
  }

  @Override
  public RenderTypeGroup getRenderType(ResourceLocation name) {
    return base.getRenderType(name);
  }
}
