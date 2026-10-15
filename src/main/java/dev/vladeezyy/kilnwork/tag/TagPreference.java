package dev.vladeezyy.kilnwork.tag;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

import java.util.Comparator;
import java.util.Optional;

/**
 * Picks one entry from a tag: vanilla first, then by mod id, so the choice is stable between runs
 */
public final class TagPreference {
  private TagPreference() {}

  @SuppressWarnings("unchecked")
  public static <T> Optional<T> getPreference(TagKey<T> tag) {
    Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(tag.registry().identifier());
    if (registry == null) {
      return Optional.empty();
    }
    return registry.get(tag).flatMap(set -> set.stream()
      .min(Comparator.comparing((Holder<T> holder) -> holder.unwrapKey().map(key -> rank(key.identifier())).orElse(Integer.MAX_VALUE))
        .thenComparing(holder -> holder.unwrapKey().map(key -> key.identifier().toString()).orElse("")))
      .map(Holder::value));
  }

  private static int rank(Identifier id) {
    return "minecraft".equals(id.getNamespace()) ? 0 : 1;
  }
}
