package dev.vladeezyy.kilnwork.data;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.util.ExtraCodecs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.vladeezyy.kilnwork.Kilnwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Data per block state, like a blockstate JSON: {@code {"variants": {"": "<id of a JSON in the folder>", "facing=down": {...}}}}.
 * A variant is either an inline value or the id of another JSON in the same folder. Later variants win.
 */
public class BlockStateDataLoader<T> extends SimpleJsonResourceReloadListener<JsonElement> {
  private final String name;
  private final String folder;
  private Map<BlockState, T> data = Map.of();

  public BlockStateDataLoader(String name, String folder) {
    super(ExtraCodecs.JSON, FileToIdConverter.json(folder));
    this.name = name;
    this.folder = folder;
  }

  public String getFolder() {
    return folder;
  }

  /**
   * Parses one value. Called before the blocks are read, with all JSON of the folder.
   * @param jsons    All JSON of the folder, for parents
   * @param id       Location of the value
   * @param element  JSON of the value
   */
  protected T parse(Map<Identifier, JsonElement> jsons, Identifier id, JsonElement element) {
    throw new UnsupportedOperationException("Override parse or use BlockStateDataLoader.of");
  }

  /** Hook before the blocks are parsed, such as reading a default value */
  protected void prepare(Map<Identifier, JsonElement> jsons) {}

  /** Loader parsing values with a codec */
  public static <T> BlockStateDataLoader<T> of(String name, String folder, com.mojang.serialization.Codec<T> codec) {
    return new BlockStateDataLoader<>(name, folder) {
      @Override
      protected T parse(Map<Identifier, JsonElement> jsons, Identifier id, JsonElement element) {
        return codec.parse(JsonOps.INSTANCE, element).getOrThrow();
      }
    };
  }

  @Override
  protected void apply(Map<Identifier, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
    prepare(jsons);
    Map<BlockState, T> map = new HashMap<>();
    Map<Identifier, T> references = new HashMap<>();
    for (Map.Entry<Identifier, JsonElement> entry : jsons.entrySet()) {
      Identifier id = entry.getKey();
      Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
      if (block == null) {
        continue;
      }
      try {
        JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), id.toString());
        if (json.size() == 0) {
          continue;
        }
        JsonObject variants = GsonHelper.getAsJsonObject(json, "variants");
        for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
          JsonElement value = variant.getValue();
          T parsed;
          if (value.isJsonPrimitive()) {
            Identifier reference = Identifier.parse(value.getAsString());
            parsed = references.get(reference);
            if (parsed == null) {
              JsonElement referenced = jsons.get(reference);
              if (referenced == null) {
                throw new IllegalArgumentException("Missing " + reference + " used by " + id);
              }
              parsed = parse(jsons, reference, referenced);
              references.put(reference, parsed);
            }
          } else {
            parsed = parse(jsons, id, value);
          }
          Predicate<BlockState> predicate = variantPredicate(block, variant.getKey());
          for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            if (predicate.test(state)) {
              map.put(state, parsed);
            }
          }
        }
      } catch (RuntimeException e) {
        Kilnwork.LOG.error("Failed to load {} for {}", name, id, e);
      }
    }
    data = map;
  }

  /** Predicate for a variant string such as {@code facing=down,extension=true}, empty for every state */
  private static Predicate<BlockState> variantPredicate(Block block, String variant) {
    Predicate<BlockState> predicate = state -> true;
    if (variant.isEmpty()) {
      return predicate;
    }
    for (String part : variant.split(",")) {
      String[] keyValue = part.split("=", 2);
      Property<?> property = block.getStateDefinition().getProperty(keyValue[0].trim());
      if (property == null || keyValue.length != 2) {
        throw new IllegalArgumentException("Invalid variant " + variant + " for " + block);
      }
      predicate = predicate.and(matches(property, keyValue[1].trim(), variant, block));
    }
    return predicate;
  }

  private static <V extends Comparable<V>> Predicate<BlockState> matches(Property<V> property, String value, String variant, Block block) {
    V parsed = property.getValue(value).orElseThrow(() -> new IllegalArgumentException("Invalid value in " + variant + " for " + block));
    return state -> state.getValue(property).equals(parsed);
  }

  @Nullable
  public T get(BlockState state) {
    return data.get(state);
  }

  public T get(BlockState state, T fallback) {
    return data.getOrDefault(state, fallback);
  }
}
