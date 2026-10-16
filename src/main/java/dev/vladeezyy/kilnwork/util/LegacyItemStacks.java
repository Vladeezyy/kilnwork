package dev.vladeezyy.kilnwork.util;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;

/**
 * Reads item stacks saved by 1.20 inside modded block entities. The world upgrade only fixes items of block entities
 * it knows, so a mod's inventory keeps the 1.20 format ({@code Count}, {@code tag}) and {@link ItemStack#parse} drops it.
 */
public final class LegacyItemStacks {
  private LegacyItemStacks() {}

  /** Data version of Minecraft 1.20.1 */
  public static final int DATA_VERSION_1_20_1 = 3465;

  /** True if the tag is an item stack in the format before 1.20.5 */
  public static boolean isLegacy(CompoundTag tag) {
    return tag.contains("Count", Tag.TAG_ANY_NUMERIC) && !tag.contains("count");
  }

  /** Parses a stack of either format, empty if invalid */
  public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag) {
    if (isLegacy(tag)) {
      Tag fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, tag),
        DATA_VERSION_1_20_1, SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
      if (fixed instanceof CompoundTag fixedTag) {
        tag = fixedTag;
      }
    }
    return ItemStack.parseOptional(registries, tag);
  }
}
