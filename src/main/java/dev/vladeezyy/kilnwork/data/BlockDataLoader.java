package dev.vladeezyy.kilnwork.data;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.util.ExtraCodecs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.vladeezyy.kilnwork.Kilnwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Data per block from JSON named after the block id: {@code <folder>/<block namespace>/<block path>.json}.
 * Other JSONs in the folder can be parents (see {@link JsonParents}); an empty object disables the block.
 */
public class BlockDataLoader<T> extends SimpleJsonResourceReloadListener<JsonElement> {
  private final String name;
  private final String folder;
  private final Codec<T> codec;
  private Map<Block, T> data = Map.of();

  public BlockDataLoader(String name, String folder, Codec<T> codec) {
    super(ExtraCodecs.JSON, FileToIdConverter.json(folder));
    this.name = name;
    this.folder = folder;
    this.codec = codec;
  }

  public String getFolder() {
    return folder;
  }

  @Override
  protected void apply(Map<Identifier, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
    Map<Block, T> map = new HashMap<>();
    Map<Identifier, T> parsed = new HashMap<>();
    for (Map.Entry<Identifier, JsonElement> entry : jsons.entrySet()) {
      Identifier id = entry.getKey();
      Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
      if (block == null) {
        continue; // template or a block of a missing mod
      }
      try {
        JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), id.toString());
        if (json.size() == 0) {
          continue;
        }
        JsonParents.Resolved resolved = JsonParents.resolve(jsons, id, json);
        T value = parsed.get(resolved.location());
        if (value == null) {
          value = codec.parse(JsonOps.INSTANCE, resolved.json()).getOrThrow();
          parsed.put(resolved.location(), value);
        }
        map.put(block, value);
      } catch (RuntimeException e) {
        Kilnwork.LOG.error("Failed to load {} for {}", name, id, e);
      }
    }
    data = map;
    Kilnwork.LOG.debug("Loaded {} {}", map.size(), name);
  }

  @Nullable
  public T get(Block block) {
    return data.get(block);
  }
}
