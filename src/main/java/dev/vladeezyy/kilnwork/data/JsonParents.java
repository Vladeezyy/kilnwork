package dev.vladeezyy.kilnwork.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Inheritance for data JSON: {@code "parent": "<id>"} copies the keys missing from the parent JSON in the same folder.
 * A JSON with only a parent is a plain redirect to it.
 */
public final class JsonParents {
  private JsonParents() {}

  /**
   * A resolved JSON and the location it came from, so redirects to the same parent can share the parsed value
   * @param location  Location of the JSON that was finally used
   * @param json      JSON with all parent data
   */
  public record Resolved(ResourceLocation location, JsonObject json) {}

  public static Resolved resolve(Map<ResourceLocation, JsonElement> jsons, ResourceLocation location, JsonObject json) {
    return resolve(jsons, location, json.deepCopy(), new ArrayList<>());
  }

  private static Resolved resolve(Map<ResourceLocation, JsonElement> jsons, ResourceLocation location, JsonObject json, List<ResourceLocation> stack) {
    while (json.has("parent")) {
      ResourceLocation parentId = ResourceLocation.parse(GsonHelper.getAsString(json, "parent"));
      stack.add(location);
      if (stack.contains(parentId)) {
        throw new JsonSyntaxException("Parent loop at " + parentId + ": " + stack);
      }
      JsonElement parentElement = jsons.get(parentId);
      if (parentElement == null) {
        throw new JsonSyntaxException("Missing parent " + parentId + " for " + location);
      }
      JsonObject parent = GsonHelper.convertToJsonObject(parentElement, parentId.toString()).deepCopy();
      if (json.size() == 1) {
        // redirect: continue with the parent itself
        json = parent;
        location = parentId;
      } else {
        parent = resolve(jsons, parentId, parent, stack).json();
        json.remove("parent");
        for (Map.Entry<String, JsonElement> entry : parent.entrySet()) {
          if (!json.has(entry.getKey())) {
            json.add(entry.getKey(), entry.getValue());
          }
        }
        break;
      }
    }
    return new Resolved(location, json);
  }
}
