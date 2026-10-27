package dev.vladeezyy.kilnwork.fabric;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Old ids of renamed registry entries ({@code Registrar#addAlias}), looked up by MappedRegistryMixin */
public final class RegistryAliases {
  private RegistryAliases() {}

  private static final Map<ResourceLocation, Map<ResourceLocation, ResourceLocation>> ALIASES = new ConcurrentHashMap<>();

  public static void add(ResourceKey<?> registry, ResourceLocation from, ResourceLocation to) {
    ALIASES.computeIfAbsent(registry.location(), k -> new ConcurrentHashMap<>()).put(from, to);
  }

  public static ResourceLocation resolve(ResourceKey<?> registry, ResourceLocation id) {
    Map<ResourceLocation, ResourceLocation> aliases = ALIASES.get(registry.location());
    return aliases == null ? id : aliases.getOrDefault(id, id);
  }
}
