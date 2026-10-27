package dev.vladeezyy.kilnwork.fabric;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Old ids of renamed registry entries ({@code Registrar#addAlias}), looked up by MappedRegistryMixin */
public final class RegistryAliases {
  private RegistryAliases() {}

  private static final Map<Identifier, Map<Identifier, Identifier>> ALIASES = new ConcurrentHashMap<>();

  public static void add(ResourceKey<?> registry, Identifier from, Identifier to) {
    ALIASES.computeIfAbsent(registry.identifier(), k -> new ConcurrentHashMap<>()).put(from, to);
  }

  public static Identifier resolve(ResourceKey<?> registry, Identifier id) {
    Map<Identifier, Identifier> aliases = ALIASES.get(registry.identifier());
    return aliases == null ? id : aliases.getOrDefault(id, id);
  }
}
