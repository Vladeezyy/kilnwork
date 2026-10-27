package dev.vladeezyy.kilnwork.tag;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
    Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(tag.registry().location());
    if (registry == null) {
      return Optional.empty();
    }
    return registry.getTag(tag).flatMap(set -> set.stream()
      .min(Comparator.comparing((Holder<T> holder) -> holder.unwrapKey().map(key -> rank(key.location())).orElse(Integer.MAX_VALUE))
        .thenComparing(holder -> holder.unwrapKey().map(key -> key.location().toString()).orElse("")))
      .map(Holder::value));
  }

  private static int rank(ResourceLocation id) {
    return "minecraft".equals(id.getNamespace()) ? 0 : 1;
  }
}
