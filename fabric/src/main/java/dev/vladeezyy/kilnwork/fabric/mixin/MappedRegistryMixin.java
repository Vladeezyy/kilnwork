package dev.vladeezyy.kilnwork.fabric.mixin;

import dev.vladeezyy.kilnwork.fabric.RegistryAliases;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Registry aliases on Fabric (NeoForge has them): an old id of a renamed entry finds the new entry */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> {
  @Shadow
  @Final
  private ResourceKey<? extends Registry<T>> key;

  @Shadow
  public abstract boolean containsKey(Identifier id);

  @ModifyVariable(method = "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;", at = @At("HEAD"), argsOnly = true)
  private Identifier kilnwork$alias(Identifier id) {
    return id != null && !containsKey(id) ? RegistryAliases.resolve(key, id) : id;
  }

  @ModifyVariable(method = "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;", at = @At("HEAD"), argsOnly = true)
  private Identifier kilnwork$aliasHolder(Identifier id) {
    return id != null && !containsKey(id) ? RegistryAliases.resolve(key, id) : id;
  }
}
